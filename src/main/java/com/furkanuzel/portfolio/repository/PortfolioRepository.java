package com.furkanuzel.portfolio.repository;

import com.furkanuzel.portfolio.model.Position;
import com.furkanuzel.portfolio.model.TradeType;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Positions and settings in an embedded H2 database. All SQL lives here. */
public final class PortfolioRepository {

    private static final String POSITION_COLUMNS =
            "ticker, yahoo_symbol, currency, quantity, average_cost, target_price";
    private static final double EPSILON = 0.000001;

    private final String jdbcUrl;

    public PortfolioRepository(Path dataDirectory) {
        String databasePath = dataDirectory.resolve("portfolio-db").toAbsolutePath().toString().replace('\\', '/');
        this.jdbcUrl = "jdbc:h2:file:" + databasePath + ";AUTO_SERVER=TRUE";
        initialize();
    }

    private Connection connect() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 sürücüsü yüklenemedi", e);
        }
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    private void initialize() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS positions (
                        ticker        VARCHAR(12) PRIMARY KEY,
                        yahoo_symbol  VARCHAR(30) NOT NULL,
                        currency      VARCHAR(3)  NOT NULL,
                        quantity      DOUBLE PRECISION NOT NULL,
                        average_cost  DOUBLE PRECISION NOT NULL,
                        target_price  DOUBLE PRECISION DEFAULT 0 NOT NULL
                    )""");
            s.executeUpdate("ALTER TABLE positions ADD COLUMN IF NOT EXISTS target_price DOUBLE PRECISION DEFAULT 0 NOT NULL");
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                        setting_key   VARCHAR(60) PRIMARY KEY,
                        setting_value DOUBLE PRECISION NOT NULL
                    )""");
        } catch (SQLException e) {
            throw new IllegalStateException("Veritabanı başlatılamadı", e);
        }
    }

    public List<Position> findAll() {
        List<Position> positions = new ArrayList<>();
        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT " + POSITION_COLUMNS + " FROM positions ORDER BY ticker")) {
            while (r.next()) positions.add(readPosition(r));
            return positions;
        } catch (SQLException e) {
            throw new IllegalStateException("Pozisyonlar okunamadı", e);
        }
    }

    public boolean isEmpty() {
        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM positions")) {
            return r.next() && r.getInt(1) == 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Pozisyonlar okunamadı", e);
        }
    }

    /**
     * Applies a buy or sell. Buying recalculates the weighted average cost; selling keeps it
     * and only reduces the quantity. A fully sold position stays at zero lots until removed.
     */
    public synchronized void applyTrade(String ticker, String symbol, String currency,
                                        TradeType type, double quantity, double price) {
        try (Connection c = connect()) {
            Position old = find(c, ticker);
            if (type == TradeType.BUY) {
                double oldQuantity = old == null ? 0 : old.quantity();
                double oldCost = old == null ? 0 : old.averageCost();
                double newQuantity = oldQuantity + quantity;
                double newAverage = (oldQuantity * oldCost + quantity * price) / newQuantity;
                upsert(c, new Position(ticker, symbol, currency, newQuantity, newAverage,
                        old == null ? 0 : old.targetPrice()));
            } else {
                if (old == null || quantity > old.quantity() + EPSILON) {
                    throw new IllegalArgumentException("Satış adedi mevcut lottan fazla");
                }
                double remaining = Math.max(0, old.quantity() - quantity);
                upsert(c, new Position(ticker, old.yahooSymbol(), old.currency(), remaining,
                        old.averageCost(), old.targetPrice()));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("İşlem kaydedilemedi", e);
        }
    }

    public synchronized void insert(Position position) {
        try (Connection c = connect()) {
            upsert(c, position);
        } catch (SQLException e) {
            throw new IllegalStateException("Pozisyon kaydedilemedi", e);
        }
    }

    public synchronized void updateTargetPrice(String ticker, double targetPrice) {
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement("UPDATE positions SET target_price = ? WHERE ticker = ?")) {
            p.setDouble(1, targetPrice);
            p.setString(2, ticker);
            if (p.executeUpdate() == 0) throw new IllegalArgumentException("Hisse portföyde bulunamadı");
        } catch (SQLException e) {
            throw new IllegalStateException("Hedef fiyat kaydedilemedi", e);
        }
    }

    public double getPortfolioTarget() {
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(
                     "SELECT setting_value FROM app_settings WHERE setting_key = 'portfolio_target'");
             ResultSet r = p.executeQuery()) {
            return r.next() ? r.getDouble(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Portföy hedefi okunamadı", e);
        }
    }

    public synchronized void savePortfolioTarget(double target) {
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(
                     "MERGE INTO app_settings KEY(setting_key) VALUES ('portfolio_target', ?)")) {
            p.setDouble(1, target);
            p.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Portföy hedefi kaydedilemedi", e);
        }
    }

    /** Removes a position, but only once every lot has been sold. */
    public synchronized void deleteClosedPosition(String ticker) {
        try (Connection c = connect()) {
            Position position = find(c, ticker);
            if (position == null) throw new IllegalArgumentException("Hisse portföyde bulunamadı");
            if (Math.abs(position.quantity()) > EPSILON) {
                throw new IllegalArgumentException("Yalnızca lotu sıfır olan hisseler kaldırılabilir");
            }
            try (PreparedStatement p = c.prepareStatement("DELETE FROM positions WHERE ticker = ?")) {
                p.setString(1, ticker);
                p.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Hisse kaldırılamadı", e);
        }
    }

    private Position find(Connection c, String ticker) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "SELECT " + POSITION_COLUMNS + " FROM positions WHERE ticker = ?")) {
            p.setString(1, ticker);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? readPosition(r) : null;
            }
        }
    }

    private static Position readPosition(ResultSet r) throws SQLException {
        return new Position(r.getString(1), r.getString(2), r.getString(3),
                r.getDouble(4), r.getDouble(5), r.getDouble(6));
    }

    private static void upsert(Connection c, Position position) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "MERGE INTO positions (" + POSITION_COLUMNS + ") KEY(ticker) VALUES (?, ?, ?, ?, ?, ?)")) {
            p.setString(1, position.ticker());
            p.setString(2, position.yahooSymbol());
            p.setString(3, position.currency());
            p.setDouble(4, position.quantity());
            p.setDouble(5, position.averageCost());
            p.setDouble(6, position.targetPrice());
            p.executeUpdate();
        }
    }
}
