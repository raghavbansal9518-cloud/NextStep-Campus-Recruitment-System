package com.nextstep.dao;

import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Company;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * CompanyDAO
 * 
 * Read-focused Data Access Object (DAO) for querying hiring partner companies.
 */
public class CompanyDAO {

    /**
     * Retrieves all registered partner companies ordered by name.
     */
    public List<Company> findAllCompanies() throws SQLException {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT id, name, industry, email, location, website FROM companies ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToCompany(rs));
            }
        }
        return list;
    }

    /**
     * Finds a company by primary key ID.
     */
    public Company findById(int id) throws SQLException {
        String sql = "SELECT id, name, industry, email, location, website FROM companies WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCompany(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a company by exact name.
     */
    public Company findByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id, name, industry, email, location, website FROM companies WHERE name = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCompany(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates an existing partner company record.
     *
     * @param company Company entity with updated fields
     * @return boolean true if update succeeded
     * @throws SQLException on database error
     */
    public boolean updateCompany(Company company) throws SQLException {
        if (company == null || company.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE companies SET name = ?, industry = ?, email = ?, location = ?, website = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, company.getName().trim());
            stmt.setString(2, company.getIndustry().trim());
            stmt.setString(3, company.getEmail().trim());
            stmt.setString(4, company.getLocation().trim());
            stmt.setString(5, company.getWebsite() != null ? company.getWebsite().trim() : null);
            stmt.setInt(6, company.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    private Company mapRowToCompany(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String industry = rs.getString("industry");
        String email = rs.getString("email");
        String location = rs.getString("location");
        String website = rs.getString("website");

        return new Company(id, name, industry, email, location, website);
    }
}
