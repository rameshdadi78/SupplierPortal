package Sp.Supplierportal;


import jakarta.ws.rs.GET;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

@Path("parts")
public class ECPartInfo {

	@GET
@Path("changeActions")
@Produces(MediaType.APPLICATION_JSON)
public String getChangeActionDetails(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

    // Load properties file
    Properties pro = new Properties();
    InputStream input = getClass().getClassLoader().getResourceAsStream("CAMappingNames.properties");
    if (input == null) {
        throw new FileNotFoundException("CAMappingNames.properties file not found.");
    }
    pro.load(input);

    String allAffectedItemsTable = pro.getProperty("all_AffectedItems_Table");
    String changeActionTable = pro.getProperty("change_Action_Table");
    String[] changeActionColumnNames = pro.getProperty("change_Action_Attributes_Mapping").split(",");
    String[] allAffectedItemsColumnNames = pro.getProperty("allAffectedItems_Attributes_Mapping").split(",");

    // Map to store the column names and their display names
    Map<String, String> columnMap = new HashMap<>();
    for (String mapping : allAffectedItemsColumnNames) {
        String[] parts = mapping.split("\\|");
        if (parts.length == 2) {
            columnMap.put(parts[1].trim(), parts[0].trim());
        }
    }

    Map<String, String> changeActionMap = new HashMap<>();
    for (String mapping : changeActionColumnNames) {
        String[] parts = mapping.split("\\|");
        if (parts.length == 2) {
            changeActionMap.put(parts[1].trim(), parts[0].trim());
        }
    }

    Map<String, String> attributeDisplayNames = new HashMap<>();
    for (String key : pro.stringPropertyNames()) {
        if (key.startsWith("Attribute_")) {
            String columnName = key.substring("Attribute_".length()).toLowerCase();
            attributeDisplayNames.put(columnName, pro.getProperty(key));
        }
    }

    // Get query parameters
    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
    String partid = queryParams.getFirst("partid");

    // Construct the SQL query
    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(allAffectedItemsTable);
    if (partid != null && !partid.isEmpty()) {
        sql.append(" WHERE topartid = '").append(partid.replace("'", "''")).append("'");
    }

    ResultSet result = null;
    JSONArray jsonArray = new JSONArray();
    try {
        Class.forName("org.postgresql.Driver");
	        Connection conn = DriverManager.getConnection(url, userName, password);
	        Statement stmt = conn.createStatement();
	        result = stmt.executeQuery(sql.toString());

        result = stmt.executeQuery(sql.toString());

        while (result.next()) {
            String currentPartid = result.getString("topartid");
            String caid = result.getString("caid");

            JSONObject jsonObject = new JSONObject();

            // Fetch the change action data and relationship data
            JSONObject changeActionData = fetchChangeActionData(conn, changeActionTable, caid, changeActionMap, attributeDisplayNames);
            JSONObject relationshipData = fetchRelationshipData(conn, allAffectedItemsTable, currentPartid, columnMap);

            // Directly merge the change action data into the main JSON object
            for (String key : changeActionData.keySet()) {
                jsonObject.put(key, changeActionData.get(key));
            }

            // Directly merge the relationship data into the main JSON object
            for (String key : relationshipData.keySet()) {
                jsonObject.put(key, relationshipData.get(key));
            }

            // Add the partid after other entries
            jsonObject.put("partid", currentPartid);

            // Create the final object structure with caid
            JSONObject idObject = new JSONObject();
            idObject.put("caid: " + caid, jsonObject);
            jsonArray.put(idObject);
        }
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (result != null) {
            try {
                result.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    JSONObject finalObject = new JSONObject();
    finalObject.put("objectdetails", jsonArray);
    return finalObject.toString();
}



	// Fetch CAID from ChangeAction table for the given partid
	private JSONObject fetchChangeActionData(Connection conn, String tableName, String caid, Map<String, String> columnMappings, Map<String, String> attributeDisplayNames) throws SQLException {
	    JSONObject changeActionData = new JSONObject();
	    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE caid = '").append(caid.replace("'", "''")).append("'");

	    Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
	    ResultSet resultSet = stmt.executeQuery(sql.toString());

	    if (resultSet.next()) {
	        JSONArray basicAttributesArray = new JSONArray();
	        JSONArray attributesArray = new JSONArray();

	      
        for (String column : columnMappings.keySet()) {
            // Exclude certain attributes
            if (!Arrays.asList("caid", "direction", "revision").contains(column.toLowerCase())) {
                JSONObject attribute = new JSONObject();
                String displayName = attributeDisplayNames.getOrDefault(column, columnMappings.get(column));
                String value = resultSet.getString(column);

                attribute.put("displayName", displayName);
                attribute.put("name", column);
                attribute.put("value", value);

                if (Arrays.asList("name","type","state", "synopsis", "project", "description","owner").contains(column)) {
                    basicAttributesArray.put(attribute);
                } else {
                    attributesArray.put(attribute);
                }
            }
        }


	        changeActionData.put("BasicAttributesOfChangeAction", basicAttributesArray);
	        changeActionData.put("attributesOfCA", attributesArray);
	    }

	    resultSet.close();
	    return changeActionData;
	}

	// Fetch relationship attributes for the given partid
	private JSONObject fetchRelationshipData(Connection conn, String tableName, String topartid, Map<String, String> columnMap) throws SQLException {
	    JSONObject relationshipData = new JSONObject();
	    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE topartid = '").append(topartid.replace("'", "''")).append("'");

	    Statement stmt = conn.createStatement();
	    ResultSet resultSet = stmt.executeQuery(sql.toString());

	    while (resultSet.next()) {
	        JSONArray attributesArray = new JSONArray();
	        for (String column : columnMap.keySet()) {
	            // Exclude certain columns
	            if ("caid".equalsIgnoreCase(column) || "relationshipid".equalsIgnoreCase(column) ||
	                "topartid".equalsIgnoreCase(column) || "direction".equalsIgnoreCase(column)) {
	                continue;
	            }

	            JSONObject attribute = new JSONObject();
	            String displayName = columnMap.get(column);
	            String value = resultSet.getString(column);

	            attribute.put("displayName", displayName);
	            attribute.put("name", column);
	            attribute.put("value", value);

	            attributesArray.put(attribute);
	        }
	        relationshipData.put("relattributesOfAffectedItems", attributesArray);
	    }

	    resultSet.close();
	    stmt.close(); // Ensure the Statement is also closed to avoid resource leaks
	    return relationshipData;
	}


//http://localhost:8080/CHeck12/webapi/parts/specifications?partid=11345.12345.17745.23456
	
	
	@GET
	@Path("specifications")
	 @Produces(MediaType.APPLICATION_JSON)
	   public String getSpecificationDetails(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("CAMappingNames.properties");
        if (input == null) {
            throw new FileNotFoundException("CAMappingNames.properties file not found.");
        }
        pro.load(input);

        String specRelationshipTable = pro.getProperty("Specifications_Relationship_table");
        String specificationTable = pro.getProperty("Specification_Table");
        String[] specificationColumnNames = pro.getProperty("Specification_Attributes_Mapping").split(",");
        String[] specRelationshipColumnNames = pro.getProperty("Specification_Relationship_Attributes_Mapping").split(",");

        // Ensure property values are correctly loaded
        if (specRelationshipTable == null || specificationTable == null) {
            throw new IllegalArgumentException("Required table names are not found in properties file.");
        }

        // Map to store the column names and their display names
        Map<String, String> columnMap = new HashMap<>();
        for (String mapping : specRelationshipColumnNames) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                columnMap.put(parts[1].trim(), parts[0].trim());
            }
        }

        Map<String, String> specificationMap = new HashMap<>();
        for (String mapping : specificationColumnNames) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                specificationMap.put(parts[1].trim(), parts[0].trim());
            }
        }

        Map<String, String> attributeDisplayNames = new HashMap<>();
        for (String key : pro.stringPropertyNames()) {
            if (key.startsWith("Attribute_")) {
                String columnName = key.substring("Attribute_".length()).toLowerCase();
                attributeDisplayNames.put(columnName, pro.getProperty(key));
            }
        }

        // Get query parameters
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String partid = queryParams.getFirst("partid");

        // Check for null or empty partid
        if (partid != null && partid.trim().isEmpty()) {
            partid = null;
        }

        // Construct the SQL query
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(specRelationshipTable);
        if (partid != null) {
            sql.append(" WHERE topartid = '").append(partid.replace("'", "''")).append("'");
        }

        // Debug log for SQL statement
        System.out.println("Executing SQL: " + sql.toString());

        ResultSet result = null;
        JSONArray jsonArray = new JSONArray();
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(url, userName, password);
            Statement stmt = conn.createStatement();
            result = stmt.executeQuery(sql.toString());

            while (result.next()) {
                String currentPartid = result.getString("topartid");
                String specificationid = result.getString("specificationid");

                JSONObject jsonObject = new JSONObject();

                // Fetch the specification data and relationship data
                JSONObject specificationData = fetchSpecificationData(conn, specificationTable, specificationid, specificationMap, attributeDisplayNames);
                JSONArray relationshipData = fetchSpecRelationshipData(conn, specRelationshipTable, currentPartid, columnMap);

                // Add BasicAttributesOfSpecification and attributesOfSpec to jsonObject
                jsonObject.put("BasicAttributesOfSpecification", specificationData.getJSONArray("BasicAttributesOfSpecification"));
                jsonObject.put("attributesOfSpec", specificationData.getJSONArray("attributesOfSpec"));

                // Add relattributesOfSpec to jsonObject
                jsonObject.put("relattributesOfSpec", relationshipData);

                // Add the partid to jsonObject
                jsonObject.put("partid", currentPartid);

                // Create the final object structure with specificationid
                JSONObject idObject = new JSONObject();
                idObject.put("specificationid: " + specificationid, jsonObject);
                jsonArray.put(idObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        JSONObject finalObject = new JSONObject();
        finalObject.put("objectdetails", jsonArray);
        return finalObject.toString();
    }

    // Fetch specification data for the given specificationid
    private JSONObject fetchSpecificationData(Connection conn, String tableName, String specificationid, Map<String, String> columnMappings, Map<String, String> attributeDisplayNames) throws SQLException {
        JSONObject specificationData = new JSONObject();
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE specificationid = '").append(specificationid.replace("'", "''")).append("'");

        // Debug log for SQL statement
        System.out.println("Executing Specification SQL: " + sql.toString());

        Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet resultSet = stmt.executeQuery(sql.toString());

        if (resultSet.next()) {
            JSONArray basicAttributesArray = new JSONArray();
            JSONArray attributesArray = new JSONArray();

            // Define the set of basic attributes
            Set<String> basicAttributesSet = new HashSet<>(Arrays.asList("name", "type", "rev", "state", "title", "project", "description", "owner"));

            // Set of attributes to exclude
            Set<String> excludeAttributes = new HashSet<>(Arrays.asList("specificationid", "relid", "topartid"));

            for (String column : columnMappings.keySet()) {
                if (excludeAttributes.contains(column.toLowerCase())) {
                    continue; // Skip the excluded attributes
                }

                String value = resultSet.getString(column);
                if (value != null && !value.isEmpty()) { // Ensure the value is not null or empty
                    JSONObject attribute = new JSONObject();
                    String displayName = attributeDisplayNames.getOrDefault(column.toLowerCase(), columnMappings.get(column));

                    attribute.put("displayName", displayName);
                    attribute.put("name", column);
                    attribute.put("value", value);

                    if (basicAttributesSet.contains(column.toLowerCase())) {
                        basicAttributesArray.put(attribute);
                    } else {
                        attributesArray.put(attribute);
                    }
                }
            }

            // Debugging statements to ensure attributes are correctly identified
            System.out.println("Basic Attributes: " + basicAttributesArray.toString());
            System.out.println("Other Attributes: " + attributesArray.toString());

            specificationData.put("BasicAttributesOfSpecification", basicAttributesArray);
            specificationData.put("attributesOfSpec", attributesArray);
        }

        resultSet.close();
        return specificationData;
    }

    // Fetch relationship attributes for the given part id
    private JSONArray fetchSpecRelationshipData(Connection conn, String tableName, String topartid, Map<String, String> columnMap) throws SQLException {
        JSONArray attributesArray = new JSONArray();
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE topartid = '").append(topartid.replace("'", "''")).append("'");

        // Debug log for SQL statement
        System.out.println("Executing Relationship SQL: " + sql.toString());

        Statement stmt = conn.createStatement();
        ResultSet resultSet = stmt.executeQuery(sql.toString());

        // Set of attributes to exclude
        Set<String> excludeAttributes = new HashSet<>(Arrays.asList("relid", "specificationid", "topartid"));

        while (resultSet.next()) {
            JSONObject attributeObject = new JSONObject();
            for (String column : columnMap.keySet()) {
                if (excludeAttributes.contains(column.toLowerCase())) {
                    continue; // Skip the excluded attributes
                }

                String value = resultSet.getString(column);
                if (value != null && !value.isEmpty()) { // Ensure the value is not null or empty
                    JSONObject attribute = new JSONObject();
                    String displayName = columnMap.get(column);

                    attribute.put("displayName", displayName);
                    attribute.put("name", column);
                    attribute.put("value", value);

                    attributesArray.put(attribute);
                }
            }
        }

        resultSet.close();
        return attributesArray;
    }
	    
//FileDownload check
    @GET
    @Path("/download")
    @Produces("application/octet-stream")
    public Response downloadFile() {
        // Specify the path to your file
    	//C:\Users\lenovo\Downloads\ABC.txt
        String filePath = "C:\\Users\\DELL\\Downloads\\ABC.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("File not found at specified path.")
                    .build();
        }

        return Response.ok(file)
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                .build();
    }
    
//    //jayasimha checking for alternates

/**
 * Handles HTTP POST requests to retrieve alternate parts and their related information
 * based on the provided query parameters and JSON request body.
 *
 * @param uriInfo     URI information containing query parameters.
 * @param requestBody JSON string containing the request body.
 * @return JSON string containing the results with alternate parts and relationship attributes.
 * @throws Exception if any error occurs during the processing of the request.
 */

	    //http://localhost:8080/CHeck12/webapi/parts/alternates
	    //http://localhost:8080/CHeck12/webapi/parts/alternates?partid=1234.5678.1112.123
	    @GET
	    @Path("alternates")
	    @Produces(MediaType.APPLICATION_JSON)
	    public String getAlternateParts(@Context UriInfo uriInfo) throws Exception {
	    	String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
	        // Load properties file
	        Properties pro = new Properties();
	        InputStream input = getClass().getClassLoader().getResourceAsStream("CAMappingNames.properties");
	        if (input == null) {
	            throw new FileNotFoundException("CAMappingNames.properties file not found.");
	        }
	        pro.load(input);

	        String tablename = pro.getProperty("ecpartTable");
	        String columnsProperty = pro.getProperty("columns");
	        String[] columnMappings = columnsProperty.split(",");
	        String alternateRelTable = pro.getProperty("Alternate_Rel_Table");
	        String AlternateReffTable = pro.getProperty("Alternate_Reff_Table");
	        String[] AlternateReffAttributesMapping = pro.getProperty("Alternate_Reff_Attributes_Mapping").split(",");
	        String[] AlternateRelAttributesMapping = pro.getProperty("Alternate_Rel_Attributes_Mapping").split(",");

	        // Map to store the column names and their display names
	        Map<String, String> columnMap = new HashMap<>();
	        for (String mapping : columnMappings) {
	            String[] parts = mapping.split("\\|");
	            if (parts.length == 2) {
	                columnMap.put(parts[1].trim(), parts[0].trim());
	            }
	        }

	        Map<String, String> allAffectedItemsMap = new HashMap<>();
	        for (String mapping : AlternateRelAttributesMapping) {
	            String[] parts = mapping.split("\\|");
	            if (parts.length == 2) {
	                allAffectedItemsMap.put(parts[1].trim(), parts[0].trim());
	            }
	        }

	        Map<String, String> alternatePartsMap = new HashMap<>();
	        for (String mapping : AlternateReffAttributesMapping) {
	            String[] parts = mapping.split("\\|");
	            if (parts.length == 2) {
	                alternatePartsMap.put(parts[1].trim(), parts[0].trim());
	            }
	        }

	        Map<String, String> attributeDisplayNames = new HashMap<>();
	        for (String key : pro.stringPropertyNames()) {
	            if (key.startsWith("Attribute_")) {
	                String columnName = key.substring("Attribute_".length()).toLowerCase();
	                attributeDisplayNames.put(columnName, pro.getProperty(key));
	            }
	        }

	        // Extract query parameters
	        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
	        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tablename).append(" WHERE 1=1");
	        queryParams.forEach((key, values) -> {
	            String value = values.get(0);
	            if (value != null && !value.trim().isEmpty()) {
	                sql.append(" AND ").append(key).append(" = '").append(value.replace("'", "''")).append("'");
	            }
	        });
	        
	        ResultSet result = null;
	        JSONArray jsonArray = new JSONArray();
	        try {
	            Class.forName("org.postgresql.Driver");
	            Connection conn = DriverManager.getConnection(url, userName, password);
	            Statement stmt = conn.createStatement();
	            result = stmt.executeQuery(sql.toString());

	            while (result.next()) {
	                String fetchedPartid = result.getString("partid"); // Extract the Id value

	                // Fetch the Alternate_Id from AllAffectedItems table
	                String alternateId = fetchAlternateIdFromtable(conn, alternateRelTable, fetchedPartid);

	                JSONObject jsonObject = new JSONObject();
	                JSONArray basicAttributesArray = new JSONArray();
	                JSONArray attributesArray = new JSONArray();

	                // Process part attributes
	                for (String column : columnMap.keySet()) {
	                    JSONObject attribute = new JSONObject();
	                    String columnValue = result.getString(column);

	                    // Use the specific display name if available, otherwise fallback to columnMap value
	                    String displayName = attributeDisplayNames.getOrDefault(column, columnMap.get(column));

	                    attribute.put("displayName", displayName);
	                    attribute.put("name", column);
	                    attribute.put("value", columnValue);

	                    // Check if the column is part of basic attributes
	                    if (Arrays.asList(pro.getProperty("basicAttributes").split(",")).contains(column)) {
	                        basicAttributesArray.put(attribute);
	                    } else {
	                        attributesArray.put(attribute);
	                    }
	                }

	                jsonObject.put("basicAttributes", basicAttributesArray);
	                jsonObject.put("attributes", attributesArray);

	                JSONObject alternatePartsArray = new JSONObject();
	                if (alternateId != null) {
	                    // Fetch related data for Alternate Parts using Alternate_Id
	                    alternatePartsArray = fetchAlternatePartData(conn, AlternateReffTable, alternateId, alternatePartsMap, attributeDisplayNames);
	                } else {
	                    System.out.println("Alternate_Id is null for partid: " + fetchedPartid);
	                }

	                // Fetch related data
	                JSONObject relationshipAttributesArray = fetchAlternateRelationshipData(conn, alternateRelTable, fetchedPartid, allAffectedItemsMap);

	                jsonObject.put("alternateParts", alternatePartsArray);
	                jsonObject.put("relationshipattributes", relationshipAttributesArray);

	                JSONObject idObject = new JSONObject();
	                idObject.put("objectId: " + fetchedPartid, jsonObject); // Use Id as the key
	                jsonArray.put(idObject);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            if (result != null) {
	                try {
	                    result.close();
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        JSONObject finalObject = new JSONObject();
	        finalObject.put("results", jsonArray);
	        return finalObject.toString();
	    }

	    private String fetchAlternateIdFromtable(Connection conn, String tableName, String partid) throws SQLException {
	        String alternateId = null;
	        System.out.println("tableName-----111-----Alter-----"+tableName);
	        String query = "SELECT Alternate_Id FROM " + tableName + " WHERE topartid = ?";
	        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
	            pstmt.setString(1, partid);
	            ResultSet resultSet = pstmt.executeQuery();
	            if (resultSet.next()) {
	                alternateId = resultSet.getString("Alternate_Id");
	            }
	        }
	        return alternateId;
	    }

	    private JSONObject fetchAlternatePartData(Connection conn, String tableName, String alternateId, Map<String, String> columnMappings, Map<String, String> attributeDisplayNames) throws SQLException {
	        JSONObject alternatesPartsData = new JSONObject();
	        System.out.println("tableName-----2222-----Spec-----"+tableName);
	        String sql = "SELECT * FROM " + tableName + " WHERE Alternate_Id = ?";

	        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
	            pstmt.setString(1, alternateId);
	            ResultSet resultSet = pstmt.executeQuery();

	            while (resultSet.next()) {
	                JSONArray attributesArray = new JSONArray();
	                for (Entry<String, String> entry : columnMappings.entrySet()) {
	                    String columnName = entry.getKey();
	                    String displayName = entry.getValue();
	                    
	                    String value = resultSet.getString(columnName);

	                    JSONObject attribute = new JSONObject();
	                    attribute.put("displayName", displayName);
	                    attribute.put("name", columnName);
	                    attribute.put("value", value);

	                    attributesArray.put(attribute);
	                }

	                alternatesPartsData.put("id: " + alternateId, attributesArray);
	            }
	        }

	        return alternatesPartsData;
	    }

	    private JSONObject fetchAlternateRelationshipData(Connection conn, String tableName, String topartid, Map<String, String> columnMap) throws SQLException {
	        JSONObject relationshipData = new JSONObject();
	        String sql = "SELECT * FROM " + tableName + " WHERE topartid = ?";

	        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
	            pstmt.setString(1, topartid);
	            ResultSet resultSet = pstmt.executeQuery();

	            while (resultSet.next()) {
	                String relationshipId = resultSet.getString("relationshipId");

	                JSONArray attributesArray = new JSONArray();
	                for (Entry<String, String> entry : columnMap.entrySet()) {
	                    String columnName = entry.getKey();
	                    String displayName = entry.getValue();
	                    
	                    String value = resultSet.getString(columnName);

	                    JSONObject attribute = new JSONObject();
	                    attribute.put("displayName", displayName);
	                    attribute.put("name", columnName);
	                    attribute.put("value", value);

	                    attributesArray.put(attribute);
	                }

	                relationshipData.put("relationship id: " + relationshipId, attributesArray);
	            }
	        }

	        return relationshipData;
	    }
}
