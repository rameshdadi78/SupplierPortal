package Sp.Supplierportal;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.plaf.synth.SynthOptionPaneUI;

import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * RESTful web service for Supplier Portal EC Part retrieval and EBOM details.
 * 
 * This service provides APIs for retrieving EC Parts data and EBOM details from
 * the database. The service reads configurations from a properties file and 
 * constructs dynamic SQL queries based on the provided query parameters.
 * The results are returned in JSON format.
 */

@Path("SupplierPortal")
public class SupplierPortal {

	/**
     * Retrieves columns from the specified table in the database and returns them in a JSON format.
     *
     * This method dynamically builds a SQL query based on the query parameters provided in the request URL.
     * It uses properties loaded from the `sp.properties` file to map internal column names to display names.
     * The results are grouped into "basicAttributes" and "attributes" based on the mappings defined in the properties file.
     *
     * @param uriInfo Provides access to query parameters of the HTTP request.
     * @return A JSON string containing the results of the query, with attributes mapped to display names.
     * @throws Exception If there is an error accessing the database or processing the request.
     */
    @GET
    @Path("parts")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllColumns(@Context UriInfo uriInfo) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
        if (input == null) {
            throw new FileNotFoundException("sp.properties file not found.");
        }
        pro.load(input);

        String tablename = pro.getProperty("ecpartTable");

        // Load ecpartcolumns and ecpartbasicAttributes
        String columnsProperty = pro.getProperty("ecpartcolumns");
        String basicAttributesProperty = pro.getProperty("ecpartbasicAttributes");

        // Map to store the column names, display names, and internal names
        Map<String, Map<String, String>> columnMap = new HashMap<>();
        for (String mapping : columnsProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                columnMap.put(parts[1].trim(), detailMap);
            }
        }

        // Map to store the basic attribute column names, display names, and internal names
        Map<String, Map<String, String>> basicAttributeMap = new HashMap<>();
        for (String mapping : basicAttributesProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                basicAttributeMap.put(parts[1].trim(), detailMap);
            }
        }

        // Build the SQL query dynamically based on provided query parameters
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tablename).append(" WHERE 1=1");
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        queryParams.forEach((key, values) -> {
            String value = values.get(0);
            if (value != null && !value.trim().isEmpty()) {
                sql.append(" AND ").append(key).append(" = '").append(value).append("'");
            }
        });

        ResultSet result = null;
        JSONArray jsonArray = new JSONArray();
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Create the connection
            Connection conn = DriverManager.getConnection(url, userName, password);
            Statement stmt = conn.createStatement();
            result = stmt.executeQuery(sql.toString());

            // Debugging statement to log the SQL query
            System.out.println("Executing SQL: " + sql.toString());

            while (result.next()) {
                String id = result.getString("partid"); // Extract the Id value

                JSONObject jsonObject = new JSONObject();
                JSONArray basicAttributesArray = new JSONArray();
                JSONArray attributesArray = new JSONArray();

                // Add basic attributes
                for (String column : basicAttributeMap.keySet()) {
                    JSONObject attribute = new JSONObject();
                    String columnValue = result.getString(column);
                    Map<String, String> details = basicAttributeMap.get(column);
                    attribute.put("displayName", details.get("displayName"));
                    attribute.put("name", details.get("internalName"));
                    attribute.put("value", columnValue);
                    basicAttributesArray.put(attribute);
                }

                // Add other attributes
                for (String column : columnMap.keySet()) {
                    if (!column.equalsIgnoreCase("Id")) {
                        JSONObject attribute = new JSONObject();
                        String columnValue = result.getString(column);
                        Map<String, String> details = columnMap.get(column);
                        attribute.put("displayName", details.get("displayName"));
                        attribute.put("name", details.get("internalName"));
                        attribute.put("value", columnValue);
                        attributesArray.put(attribute);
                    }
                }

                jsonObject.put("basicAttributes", basicAttributesArray);
                jsonObject.put("attributes", attributesArray);

                JSONObject idObject = new JSONObject();
                idObject.put("objectId: " + id, jsonObject); // Use Id as the key
                jsonArray.put(idObject);
            }
        } catch (Exception e) {
            // Logging error for debugging
            System.err.println("Error during database query execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (Exception e) {
                    System.err.println("Error closing ResultSet: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        JSONObject finalObject = new JSONObject();
        finalObject.put("results", jsonArray);
        return finalObject.toString();
    }

    /**
     * Retrieves EBOM details from the specified table in the database.
     * This method dynamically builds a SQL query based on the query parameters provided.
     * It uses properties loaded from the `sp.properties` file to map internal column names to display names.
     *
     * @param fromId         The 'fromId' query parameter, which is used to filter the data.
     * @param connAttributes A boolean flag indicating whether connection attributes should be included in the response.
     * @return A JSON string containing the EBOM details, with attributes mapped to display names.
     * @throws Exception If there is an error accessing the database or processing the request.
     */
    @GET
	@Path("ebomdetails")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEBOMDetails(
	        @QueryParam("fromid") String fromId,
	        @QueryParam("connattributes") boolean connAttributes
	) throws Exception {
		String url=System.getenv("SupplierPortalSPDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    // Load properties file
	    Properties properties = new Properties();
	    try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
	        if (input == null) {
	            throw new FileNotFoundException("Property file 'sp.properties' not found in the classpath");
	        }
	        properties.load(input);
	    }

	    String ebomTable = properties.getProperty("ebomTable");
	    String connectionAttributes = properties.getProperty("ebomconnectionAttributes");
	    String basicAttributes = properties.getProperty("ebombasicAttributes");
	    String attributes = properties.getProperty("attributes");

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ");
	    sql.append(ebomTable).append(" WHERE 1=1");

	   
	    if (fromId != null && !fromId.trim().isEmpty()) {
	        sql.append(" AND fromid = '").append(fromId).append("'");
	    }

	    ResultSet result = null;
	    JSONArray jsonArray = new JSONArray();
	    try {
	        Class.forName("org.postgresql.Driver");
	        Connection conn = DriverManager.getConnection(url, userName, password);
	        Statement stmt = conn.createStatement();
	        
	        result = stmt.executeQuery(sql.toString());
	        while (result.next()) {
	            JSONObject jsonObject = new JSONObject(); // Create new JSONObject for each row
	            JSONObject objectDetails = new JSONObject();
	            JSONArray connAttrArray = new JSONArray();
	            JSONArray basicAttrArray = new JSONArray();
	            JSONArray attrArray = new JSONArray();

	            // Process connection attributes
	            if (connAttributes) {
	                for (String attrPair : connectionAttributes.split(",")) {
	                    String[] attrParts = attrPair.split("\\|");
	                    String attr = attrParts[1].trim();
	                    JSONObject attrObject = new JSONObject();
	                    String displayName = attrParts[0].trim();
	                    attrObject.put("displayName", displayName);
	                    attrObject.put("name", attr);
	                    attrObject.put("value", result.getString(attr));
	                    connAttrArray.put(attrObject);
	                }
	                objectDetails.put("connectionattributes", connAttrArray);
	            }

	            // Process basic attributes
	            for (String attrPair : basicAttributes.split(",")) {
	                String[] attrParts = attrPair.split("\\|");
	                String attr = attrParts[1].trim();
	                JSONObject attrObject = new JSONObject();
	                String displayName = attrParts[0].trim();
	                attrObject.put("displayName", displayName);
	                attrObject.put("name", attr);
	                attrObject.put("value", result.getString(attr));
	                basicAttrArray.put(attrObject);
	            }

	            // Process other attributes
	            for (String attrPair : attributes.split(",")) {
	                String[] attrParts = attrPair.split("\\|");
	                String attr = attrParts[1].trim();
	                if (attr.equalsIgnoreCase("fromid")) {
	                    // Skip processing the 'Id' column
	                    continue;
	                }
	                JSONObject attrObject = new JSONObject();
	                String displayName = attrParts[0].trim();
	                attrObject.put("displayName", displayName);
	                attrObject.put("name", attr);
	                attrObject.put("value", result.getString(attr));
	                attrArray.put(attrObject);
	            }

	            objectDetails.put("basicattributes", basicAttrArray);
	            objectDetails.put("attributes", attrArray);

	            jsonObject.put("objectid: " + result.getString("fromid"), objectDetails);
	            jsonArray.put(jsonObject);
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
	    return new JSONObject().put("results", jsonArray).toString();
	}


    /**
     * RESTful web service for Supplier Portal search functionality.
     * Allows for searching parts based on given criteria.
     * Results are returned in JSON format.
     */
  
    @POST
    @Path("search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getData(String s) throws IOException {
        // Check for invalid or null input
        if (s == null || s.trim().isEmpty() || !s.trim().startsWith("{")) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "fail");
            errorResponse.put("message", "Invalid JSON input.");
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(errorResponse.toString())
                           .type(MediaType.APPLICATION_JSON)
                           .build();
        }

        JSONObject json = new JSONObject(s);

        // Retrieve and sanitize the search text
        String text = json.optString("text", "%"); // Default to '%' if text is empty

        if (text.equals("*") || text.length() < 3) {
            // Case where '*' or fewer than 3 characters are entered
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "fail");
            errorResponse.put("message", "Please provide at least 3 characters or digits for the search.");
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(errorResponse.toString())
                           .type(MediaType.APPLICATION_JSON)
                           .build();
        }

        // Replace '*' with '%' for SQL LIKE syntax
        text = text.replace("*", "%");

        String field = json.optString("field");

        // Retrieve database credentials from environment variables
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        JSONArray jsonArray = new JSONArray();

        // Define SQL query strings outside the loop
        String sql = "";

        // Define connection and statement variables
        try (Connection con = DriverManager.getConnection(url, userName, password)) {

            if (field.equalsIgnoreCase("Everything")) {
                // Search in both 'partid' and 'name' fields
                sql = "SELECT partid, name, type FROM ec_parts_details WHERE (partid = ? OR name ILIKE ?)";
            } else if (field.equalsIgnoreCase("name")) {
                // Search by 'name' field only
                sql = "SELECT partid, name, type FROM ec_parts_details WHERE name ILIKE ?";
            }

            if (!sql.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    if (field.equalsIgnoreCase("Everything")) {
                        // For 'partid', allow only exact match, check for wildcards
                        if (text.contains("%")) {
                            // If 'partid' contains wildcards, skip the 'partid' search and return results only for 'name'
                            ps.setString(1, "");  // No 'partid' results with wildcard
                            ps.setString(2, "%" + text + "%");  // Wildcard search for 'name'
                        } else {
                            // Exact match for 'partid' and wildcard match for 'name'
                            ps.setString(1, text);  // Exact match for 'partid'
                            ps.setString(2, "%" + text + "%");  // Wildcard match for 'name'
                        }
                    } else if (field.equalsIgnoreCase("name")) {
                        // Search by name with wildcard
                        ps.setString(1, "%" + text + "%");  // Wildcard for 'name'
                    }

                    // Execute query and group results by 'type'
                    try (ResultSet set = ps.executeQuery()) {
                        Map<String, List<String>> partsByType = new HashMap<>();

                        while (set.next()) {
                            String id = set.getString("partid");
                            String type = set.getString("type");
                            partsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(id);
                        }

                        // Construct JSON output
                        for (Map.Entry<String, List<String>> entry : partsByType.entrySet()) {
                            String type = entry.getKey();
                            List<String> partIds = entry.getValue();

                            JSONObject jsonObject = new JSONObject();
                            JSONObject typeObject = new JSONObject();
                            typeObject.put("partid", String.join("|", partIds));
                            jsonObject.put("type: " + type, typeObject);
                            jsonArray.put(jsonObject);
                        }
                    }
                }
            }

            // Create final JSON response
            JSONObject js = new JSONObject();
            js.put("results", jsonArray);
            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            // Log error for debugging
            System.err.println("Error during database query execution: " + e.getMessage());
            e.printStackTrace();

            JSONObject js = new JSONObject();
            js.put("status", "fail");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(js.toString())
                           .type(MediaType.APPLICATION_JSON)
                           .build();
        }
    }


    /**
     * Retrieves supplier details by email.
     * Combines person details with supplier information based on the given email.
     *
     * @param uriInfo UriInfo object containing query parameters (e.g., email)
     * @return JSON string with supplier details or an error message if no details are found
     * @throws Exception if there is an error loading properties or querying the database
     */
    @GET
    @Path("getSupplierDetailsByEmail")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSupplierDetailsByEmail(@Context UriInfo uriInfo) throws Exception {
        // Load database credentials from environment variables
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file for column mappings
        Properties pro = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
            if (input == null) {
                throw new FileNotFoundException("sp.properties file not found.");
            }
            pro.load(input);
        }

        // Load column mappings for ca_suppliers_details attributes
        String detailsColumnsProperty = pro.getProperty("casuppliersdetailsattributes");
        Map<String, Map<String, String>> detailsColumnMap = new HashMap<>();
        for (String mapping : detailsColumnsProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                detailsColumnMap.put(parts[1].trim(), detailMap);
            }
        }

        // Load column mappings for ca_suppliers_connection_details attributes
        String connectionColumnsProperty = pro.getProperty("casuppliersconnectiondetailsattributes");
        Map<String, Map<String, String>> connectionColumnMap = new HashMap<>();
        for (String mapping : connectionColumnsProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                connectionColumnMap.put(parts[1].trim(), detailMap);
            }
        }

        // Get email address from query parameter
        String email = uriInfo.getQueryParameters().getFirst("email");
        if (email == null || email.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty email parameter\" }";
        }

        // SQL query to retrieve supplier details based on email
        String supplierDetailsQuery = "SELECT casd.*, ca.state, ca.owner, ca.description " +
                                      "FROM Supplier_Person_Details spd " +
                                      "JOIN ca_suppliers_details casd ON spd.companyid = casd.companyid " +
                                      "JOIN changeaction ca ON ca.caid = casd.changenumber " +
                                      "WHERE spd.email_address = ?";

        // Prepare the result as a JSON array
        JSONArray jsonArray = new JSONArray();
        Class.forName("org.postgresql.Driver");

        try (Connection conn = DriverManager.getConnection(url, userName, password);
             PreparedStatement supplierStmt = conn.prepareStatement(supplierDetailsQuery)) {

            // Set the email parameter
            supplierStmt.setString(1, email);
            ResultSet supplierResult = supplierStmt.executeQuery();

            // Check if result set has any rows
            if (!supplierResult.isBeforeFirst()) {
                return "{ \"error\": \"No supplier details found for email: " + email + "\" }";
            }

            // Process result set
            while (supplierResult.next()) {
                JSONObject jsonObject = new JSONObject();
                JSONArray attributesArray = new JSONArray();
                JSONArray connectionAttributesArray = new JSONArray();

                // Add ca_suppliers_details attributes
                for (String column : detailsColumnMap.keySet()) {
                    String columnValue = supplierResult.getString(column);
                    if (column.equalsIgnoreCase("companyid")) {
                        // Skip processing the 'companyid' column
                        continue;
                    }
                    // Handle null values
                    if (columnValue == null) {
                        columnValue = "";  // Set default value to an empty string
                    }

                    JSONObject attribute = new JSONObject();
                    Map<String, String> details = detailsColumnMap.get(column);
                    attribute.put("displayName", details.get("displayName"));
                    attribute.put("name", details.get("internalName"));
                    attribute.put("value", columnValue);
                    attributesArray.put(attribute);
                }

                // Add ca_suppliers_connection_details attributes
                for (String column : connectionColumnMap.keySet()) {
                    String columnValue = supplierResult.getString(column);
                    if (columnValue != null) {  // Check if the value exists
                        JSONObject attribute = new JSONObject();
                        Map<String, String> details = connectionColumnMap.get(column);
                        attribute.put("displayName", details.get("displayName"));
                        attribute.put("name", details.get("internalName"));
                        attribute.put("value", columnValue);
                        connectionAttributesArray.put(attribute);
                    }
                }

                jsonObject.put("attributes", attributesArray);
                jsonObject.put("connectionattributes", connectionAttributesArray);

                // Use companyid as the key for the result object
                JSONObject idObject = new JSONObject();
                idObject.put("companyid: " + supplierResult.getString("companyid"), jsonObject);
                jsonArray.put(idObject);
            }

        } catch (SQLException e) {
            // Log error for debugging
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
        }

        // Construct final JSON object
        JSONObject finalObject = new JSONObject();
        finalObject.put("results", jsonArray);
        return finalObject.toString();
    }

    /**
     * Retrieves change action details by caID.
     * Combines change action attributes from a specified table based on the given caID.
     *
     * @param uriInfo UriInfo object containing query parameters (e.g., caID)
     * @return JSON string with change action details or an error message if no details are found
     * @throws Exception if there is an error loading properties or querying the database
     */
    @GET
    @Path("getchangeactiondetails")
    @Produces(MediaType.APPLICATION_JSON)
    public String getChangeActionDetails(@Context UriInfo uriInfo) throws Exception {
        // Load database credentials from environment variables
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file for column mappings and table names
        Properties pro = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
            if (input == null) {
                throw new FileNotFoundException("sp.properties file not found.");
            }
            pro.load(input);
        }

        // Load table and column mappings
        String caTable = pro.getProperty("catable");
        String detailsColumnsProperty = pro.getProperty("cadetails");

        Map<String, Map<String, String>> detailsColumnMap = new HashMap<>();
        for (String mapping : detailsColumnsProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                detailsColumnMap.put(parts[1].trim(), detailMap);
            }
        }

        // Get caID from query parameter
        String caID = uriInfo.getQueryParameters().getFirst("caid");
        if (caID == null || caID.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty caID parameter\" }";
        }

        // SQL query to fetch change action details
        String caDetailsQuery = "SELECT * FROM " + caTable + " ca WHERE ca.caid = ?";

        JSONArray jsonArray = new JSONArray();
        Class.forName("org.postgresql.Driver");

        try (Connection conn = DriverManager.getConnection(url, userName, password);
             PreparedStatement stmt = conn.prepareStatement(caDetailsQuery)) {

            // Set the caID parameter in the query
            stmt.setString(1, caID);
            ResultSet result = stmt.executeQuery();

            // Process the result set
            while (result.next()) {
                JSONObject jsonObject = new JSONObject();
                JSONArray attributesArray = new JSONArray();

                // Add attributes from ca_suppliers_details
                for (String column : detailsColumnMap.keySet()) {
                    String columnValue = result.getString(column);

                    // Skip processing if no value for this column
                    if (columnValue == null) continue;

                    JSONObject attribute = new JSONObject();
                    Map<String, String> details = detailsColumnMap.get(column);
                    attribute.put("displayName", details.get("displayName"));
                    attribute.put("name", details.get("internalName"));
                    attribute.put("value", columnValue);
                    attributesArray.put(attribute);
                }

                jsonObject.put("attributes", attributesArray);

                // Use caID as the key for the result object
                JSONObject idObject = new JSONObject();
                idObject.put("id: " + result.getString("caid"), jsonObject);
                jsonArray.put(idObject);
            }
        } catch (SQLException e) {
            // Log error for debugging
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
        }

        // Construct final JSON object
        JSONObject finalObject = new JSONObject();
        finalObject.put("results", jsonArray);
        return finalObject.toString();
    }

    /**
     * Updates acknowledgment information in the ca_suppliers_details table.
     *
     * @param jsonInput JSON string containing 'username', 'objectId', and 'value' (true/false).
     * @return Response indicating whether the update was successful or an error occurred.
     * @throws ClassNotFoundException if the PostgreSQL Driver is not found.
     */
    @POST
    @Path("updateAcknowledgedInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setAcknowledgedInfo(String jsonInput) throws ClassNotFoundException {
        // Database connection parameters
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file for table name
        Properties pro = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
            if (input == null) {
                throw new FileNotFoundException("sp.properties file not found.");
            }
            pro.load(input);
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error loading properties file: " + e.getMessage())
                           .build();
        }

        // Retrieve table name from properties
        String tablename = pro.getProperty("casuppliersdetailsTable");

        // Parse the JSON input
        JSONObject jsonObject = new JSONObject(jsonInput);
        String acknowledgedBy = jsonObject.getString("username");
        
        // Extract username by removing everything after the '@' in the email
        String username = acknowledgedBy.split("@")[0];

        String objectId = jsonObject.getString("objectId");
        String value = jsonObject.getString("value");

        // Convert boolean "true"/"false" to "Yes"/"No"
        if ("true".equals(value)) {
            value = "Yes";
        } else if ("false".equals(value)) {
            value = "No";
        }

        // SQL update statement
        String updateSQL = "UPDATE " + tablename + 
                           " SET acknowledge = ?, acknowledgedby = ? " +
                           "WHERE changenumber = ?";

        // Initialize database driver and execute the update
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

            // Set values in the prepared statement
            pstmt.setString(1, value);      // Acknowledge value
            pstmt.setString(2, username);   // Extracted username
            pstmt.setString(3, objectId);   // Change number (objectId)

            // Execute update and check result
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                return Response.ok("Update successful").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("No records updated").build();
            }
        } catch (SQLException e) {
            // Log error for debugging purposes
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Database error: " + e.getMessage())
                           .build();
        }
    }

    /**
     * Fetches affected items for a given change action ID (caid).
     *
     * @param uriInfo URI information containing the caid query parameter.
     * @return JSON response with affected items or error message.
     * @throws Exception if any error occurs during data fetching.
     */
    @GET
    @Path("getcaaffectedItems")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCaAffectedItems(@Context UriInfo uriInfo) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
            if (input == null) {
                throw new FileNotFoundException("sp.properties file not found.");
            }
            pro.load(input);
        }

        String tablename = pro.getProperty("PartTable");
        String supplierTable = pro.getProperty("Ca_supplier_Table");

        // Load partcolumns and partbasicAttributes
        String columnsProperty = pro.getProperty("partcolumns");
        String basicAttributesProperty = pro.getProperty("partbasicAttributes");

        // Prepare column maps
        Map<String, Map<String, String>> columnMap = new HashMap<>();
        for (String mapping : columnsProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                columnMap.put(parts[1].trim(), detailMap);
            }
        }

        Map<String, Map<String, String>> basicAttributeMap = new HashMap<>();
        for (String mapping : basicAttributesProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                basicAttributeMap.put(parts[1].trim(), detailMap);
            }
        }

        String caId = uriInfo.getQueryParameters().getFirst("caid");
        if (caId == null || caId.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty caID parameter\" }";
        }

        // SQL query for fetching CA affected items
        String caDetailsQuery = "SELECT epd.*, csd.supplier_visibility, csd.supplier_item_visibility, csd.supplier_spec_visibility " +
                "FROM " + tablename + " epd " +
                "JOIN " + supplierTable + " csd " +
                "ON epd.changenumber = csd.changenumber " +
                "WHERE epd.changenumber = ?";

        JSONArray jsonArray = new JSONArray();
        Class.forName("org.postgresql.Driver");

        try (Connection conn = DriverManager.getConnection(url, userName, password);
             PreparedStatement Stmt = conn.prepareStatement(caDetailsQuery)) {

            // Set the caID parameter
            Stmt.setString(1, caId);
            ResultSet result = Stmt.executeQuery();

            // Process result set
            while (result.next()) {
                String id = result.getString("partid");

                JSONObject jsonObject = new JSONObject();
                JSONArray basicAttributesArray = new JSONArray();
                JSONArray attributesArray = new JSONArray();

                // Add basic attributes
                for (String column : basicAttributeMap.keySet()) {
                    JSONObject attribute = new JSONObject();
                    String columnValue = result.getString(column);
                    Map<String, String> details = basicAttributeMap.get(column);
                    attribute.put("displayName", details.get("displayName"));
                    attribute.put("name", details.get("internalName"));
                    attribute.put("value", columnValue);
                    basicAttributesArray.put(attribute);
                }

                // Add other attributes
                for (String column : columnMap.keySet()) {
                    JSONObject attribute = new JSONObject();
                    String columnValue = result.getString(column);
                    Map<String, String> details = columnMap.get(column);
                    attribute.put("displayName", details.get("displayName"));
                    attribute.put("name", details.get("internalName"));
                    attribute.put("value", columnValue);
                    attributesArray.put(attribute);
                }

                jsonObject.put("basicAttributes", basicAttributesArray);
                jsonObject.put("attributes", attributesArray);

                JSONObject idObject = new JSONObject();
                idObject.put("objectId: " + id, jsonObject);
                jsonArray.put(idObject);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }

        JSONObject finalObject = new JSONObject();
        finalObject.put("results", jsonArray);
        return finalObject.toString();
    }
	
    /**
     * Fetches supplier data for given object IDs and change action ID (caid).
     *
     * @param inputJson JSON string containing object IDs and caid.
     * @return JSON response with supplier data or error message.
     * @throws Exception if any error occurs during data fetching.
     */
    @POST
    @Path("getSupplierData")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getSupplierData(String inputJson) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
            if (input == null) {
                throw new FileNotFoundException("sp.properties file not found.");
            }
            pro.load(input);
        }

        String mpnTable = pro.getProperty("MpnTable");
        String connectionTableName = pro.getProperty("MpnconnectionTable");
        String caSuppliersTable = pro.getProperty("Ca_supplier_Table");

        Class.forName("org.postgresql.Driver");

        // Parse input JSON
        JSONObject inputObject = new JSONObject(inputJson);
        String objectIds = inputObject.getString("objectIds");
        String caid = inputObject.getString("caid");

        String[] objectIdsArray = objectIds.contains("|") ? objectIds.split("\\|") : new String[]{objectIds};

        // Prepare SQL queries
        String sql1 = "SELECT mpn.ManufacturerName " +
                      "FROM " + connectionTableName + " AS related_parts " +
                      "JOIN " + mpnTable + " AS mpn ON related_parts.MPNID = mpn.MPNID " +
                      "WHERE related_parts.Partid = ?";

        String sql2 = "SELECT supplier_visibility, supplier_item_visibility, supplier_spec_visibility " +
                      "FROM " + caSuppliersTable + " WHERE changenumber = ? AND company_name = ?";

        String sql3 = "SELECT supplier_visibility, supplier_item_visibility, supplier_spec_visibility " +
                      "FROM " + caSuppliersTable + " WHERE (supplier_item_visibility LIKE ? OR supplier_spec_visibility LIKE ?)";

        // JSON response structure
        JSONObject jsonResponse = new JSONObject();
        JSONArray resultsArray = new JSONArray();

        try (Connection conn = DriverManager.getConnection(url, userName, password);
             PreparedStatement joinStmt = conn.prepareStatement(sql1);
             PreparedStatement ps2 = conn.prepareStatement(sql2);
             PreparedStatement ps3 = conn.prepareStatement(sql3)) {

            // Loop through each object ID
            for (String partid : objectIdsArray) {
                joinStmt.setString(1, partid);
                ResultSet joinResultSet = joinStmt.executeQuery();

                JSONObject resultObject = new JSONObject();
                JSONObject idObject = new JSONObject(); // This will hold the attributes under the part ID
                JSONObject attributes = new JSONObject(); // Initialize attributes with default values

                // Default attribute values
                attributes.put("supplier", false);
                attributes.put("supplieritem", false);
                attributes.put("supplierspec", false);

                if (joinResultSet.next()) {
                    String manufacturerName = joinResultSet.getString("manufacturername");

                    ps2.setString(1, caid);  // Set changenumber (caid)
                    ps2.setString(2, manufacturerName);  // Set suppliername
                    ResultSet res1 = ps2.executeQuery();

                    if (res1.next()) {
                        // Retrieve visibility values from the result set
                        String supplierVisibility = res1.getString("supplier_visibility");
                        String supplierItemVisibility = res1.getString("supplier_item_visibility");
                        String supplierSpecVisibility = res1.getString("supplier_spec_visibility");

                        attributes.put("supplier", true);
                        attributes.put("supplier_visibility", supplierVisibility);
                        attributes.put("supplier_item_visibility", supplierItemVisibility);
                        attributes.put("supplier_spec_visibility", supplierSpecVisibility);
                    } else {
                        // Prepare to check if partid exists in supplier_item_visibility or supplier_spec_visibility
                        String partidLike = "%" + partid + "%";
                        ps3.setString(1, partidLike);
                        ps3.setString(2, partidLike);
                        ResultSet res2 = ps3.executeQuery();

                        if (res2.next()) {
                            // Retrieve visibility values from the result set
                            String supplierVisibility = res2.getString("supplier_visibility");
                            String supplierItemVisibility = res2.getString("supplier_item_visibility");
                            String supplierSpecVisibility = res2.getString("supplier_spec_visibility");

                            // Set true if supplier_item_visibility or supplier_spec_visibility matched
                            if (supplierItemVisibility.contains(partid) || supplierSpecVisibility.contains(partid)) {
                                attributes.put("supplieritem", true);
                                attributes.put("supplierspec", true);
                            }

                            attributes.put("supplier_visibility", supplierVisibility);
                            attributes.put("supplier_item_visibility", supplierItemVisibility);
                            attributes.put("supplier_spec_visibility", supplierSpecVisibility);
                        }
                    }
                }

                // Attach attributes to the idObject with partid as the key
                idObject.put("attributes", new JSONArray().put(attributes));
                resultObject.put("id: " + partid, idObject);
                resultsArray.put(resultObject);
            }

            jsonResponse.put("results", resultsArray);
            return jsonResponse.toString();

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage()); // Helpful for debugging
            throw new Exception("Error fetching supplier data", e);
        }
    }
	 	
    /**
     * Retrieves deviation details for a supplier by email.
     *
     * @param uriInfo Contains the query parameters, including the "email".
     * @return JSON string containing supplier and deviation details or an error message.
     * @throws Exception If any issue occurs during data retrieval.
     */
    @GET
    @Path("getDevaitionDetailsByEmail")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDevaitionDetailsByEmail(@Context UriInfo uriInfo) throws Exception {
        // Retrieve environment variables for database connection
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
        if (input == null) {
            throw new FileNotFoundException("sp.properties file not found.");
        }
        pro.load(input);

        // Load column mappings for ca_suppliers_details attributes
        String detailsColumnsProperty = pro.getProperty("casuppliersdetailsattributes");
        String supplierTable = pro.getProperty("supplierpersondetailsTable");
        String caSupplierTable = pro.getProperty("casuppliersdetailsTable");
        String devaitionTable = pro.getProperty("deviationTable");

        // Parse details columns and map internal names to display names
        Map<String, Map<String, String>> detailsColumnMap = new HashMap<>();
        for (String mapping : detailsColumnsProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                detailsColumnMap.put(parts[1].trim(), detailMap);
            }
        }

        // Load column mappings for ca_suppliers_connection_details attributes
        String connectionColumnsProperty = pro.getProperty("casuppliersconnectiondetailsattributes");
        Map<String, Map<String, String>> connectionColumnMap = new HashMap<>();
        for (String mapping : connectionColumnsProperty.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                connectionColumnMap.put(parts[1].trim(), detailMap);
            }
        }

        // Get email address from query parameter
        String email = uriInfo.getQueryParameters().getFirst("email");
        if (email == null || email.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty email parameter\" }";
        }

        // SQL query combining both person and supplier details using JOIN
        String supplierDetailsQuery = "SELECT casd.*, dev.state, dev.owner, dev.description " +
                "FROM " + supplierTable + " spd " +
                "JOIN " + caSupplierTable + " casd ON spd.companyid = casd.companyid " +
                "JOIN " + devaitionTable + " dev ON dev.deviationid = casd.changenumber " +
                "WHERE spd.email_address = ?";

        JSONArray jsonArray = new JSONArray();

        // Load PostgreSQL driver
        Class.forName("org.postgresql.Driver");

        // Database connection and query execution
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             PreparedStatement supplierStmt = conn.prepareStatement(supplierDetailsQuery)) {

            // Set the email parameter
            supplierStmt.setString(1, email);
            ResultSet supplierResult = supplierStmt.executeQuery();

            // Check if result set has any rows
            if (!supplierResult.isBeforeFirst()) {
                return "{ \"error\": \"No supplier details found for email: " + email + "\" }";
            }

            // Process result set and build JSON response
            while (supplierResult.next()) {
                JSONObject jsonObject = new JSONObject();
                JSONArray attributesArray = new JSONArray();
                JSONArray connectionAttributesArray = new JSONArray();

                // Add ca_suppliers_details attributes
                for (String column : detailsColumnMap.keySet()) {
                    String columnValue = supplierResult.getString(column);
                    if (column.equalsIgnoreCase("companyid")) {
                        // Skip processing the 'companyid' column
                        continue;
                    }
                    // Handle null values
                    if (columnValue == null) {
                        columnValue = "";  // set default value to an empty string
                    }

                    JSONObject attribute = new JSONObject();
                    Map<String, String> details = detailsColumnMap.get(column);
                    attribute.put("displayName", details.get("displayName"));
                    attribute.put("name", details.get("internalName"));
                    attribute.put("value", columnValue);
                    attributesArray.put(attribute);
                }

                // Add ca_suppliers_connection_details attributes
                for (String column : connectionColumnMap.keySet()) {
                    String columnValue = supplierResult.getString(column);
                    if (columnValue != null) {  // Check if the value exists
                        JSONObject attribute = new JSONObject();
                        Map<String, String> details = connectionColumnMap.get(column);
                        attribute.put("displayName", details.get("displayName"));
                        attribute.put("name", details.get("internalName"));
                        attribute.put("value", columnValue);
                        connectionAttributesArray.put(attribute);
                    }
                }

                jsonObject.put("attributes", attributesArray);
                jsonObject.put("connectionattributes", connectionAttributesArray);

                JSONObject idObject = new JSONObject();
                idObject.put("companyid: " + supplierResult.getString("companyid"), jsonObject);  // Use companyid as the key
                jsonArray.put(idObject);
            }
        } catch (SQLException e) {
            // Debugging output in case of database error
            System.err.println("Database error: " + e.getMessage());
            return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
        }

        // Construct final JSON object for the response
        JSONObject finalObject = new JSONObject();
        finalObject.put("results", jsonArray);
        return finalObject.toString();
    }
	
    /**
     * Retrieves deviation details for the specified deviation ID.
     *
     * @param uriInfo Contains query parameters, including the "devid" (Deviation ID).
     * @return JSON string containing deviation details or an error message.
     * @throws Exception If an error occurs during the process.
     */
    
    @GET
    @Path("getDevaitionDetails")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDevaitionDetails(@Context UriInfo uriInfo) throws Exception {
        // Database connection parameters
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file and column mappings
        Properties properties = loadProperties();
        Map<String, Map<String, String>> detailsColumnMap = loadColumnMappings(properties.getProperty("Attributes_Deviation"));

        // Get the deviation ID from query parameters
        String devID = uriInfo.getQueryParameters().getFirst("devid");
        if (devID == null || devID.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty devID parameter\" }";
        }

        // SQL query to fetch deviation details
        String caDetailsQuery = "SELECT * FROM " + properties.getProperty("deviationTable") + " dev WHERE dev.deviationid = ?";

        JSONArray jsonArray = new JSONArray();
        Class.forName("org.postgresql.Driver");

        // Open the database connection and execute the query
        try (Connection connection = DriverManager.getConnection(url, userName, password);
             PreparedStatement stmt = connection.prepareStatement(caDetailsQuery)) {

            // Set the deviation ID parameter
            stmt.setString(1, devID);
            ResultSet result = stmt.executeQuery();

            // Process the result set
            while (result.next()) {
                JSONObject jsonObject = new JSONObject();
                JSONArray attributesArray = new JSONArray();

                // Add deviation details to the JSON object
                for (String column : detailsColumnMap.keySet()) {
                    String columnValue = result.getString(column);
                    if (columnValue == null) continue; // Skip null values

                    JSONObject attribute = new JSONObject();
                    Map<String, String> details = detailsColumnMap.get(column);
                    attribute.put("displayName", details.get("displayName"));
                    attribute.put("name", details.get("internalName"));
                    attribute.put("value", columnValue);
                    attributesArray.put(attribute);
                }

                // Wrap the attributes in a JSON object with deviation ID
                jsonObject.put("attributes", attributesArray);
                JSONObject idObject = new JSONObject();
                idObject.put("id: " + result.getString("deviationid"), jsonObject);
                jsonArray.put(idObject);
            }
        } catch (SQLException e) {
            // Debugging output in case of database error
            System.err.println("Database error: " + e.getMessage());
            return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
        }

        // Construct the final JSON response
        JSONObject finalObject = new JSONObject();
        finalObject.put("results", jsonArray);
        return finalObject.toString();
    }

    /**
     * Loads the properties file containing database and table configuration.
     *
     * @return Properties object loaded with values from the file.
     * @throws Exception If the properties file is not found.
     */
    private Properties loadProperties() throws Exception {
        Properties properties = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
        if (input == null) {
            throw new FileNotFoundException("sp.properties file not found.");
        }
        properties.load(input);
        return properties;
    }

    /**
     * Loads column mappings from the properties file.
     *
     * @param columnMappings String containing column mapping details from properties.
     * @return A map with column details including internal and display names.
     */
    private Map<String, Map<String, String>> loadColumnMappings(String columnMappings) {
        Map<String, Map<String, String>> columnMap = new HashMap<>();
        for (String mapping : columnMappings.split(",")) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                Map<String, String> detailMap = new HashMap<>();
                detailMap.put("internalName", parts[1].trim());
                detailMap.put("displayName", parts[0].trim());
                columnMap.put(parts[1].trim(), detailMap);
            }
        }
        return columnMap;
    }

	/**
     * Retrieves counts for deviations, change actions, and EC parts associated with the given email.
     * 
     * @param uriInfo The UriInfo context for extracting query parameters.
     * @return JSON string representing the counts of change actions, deviations, and EC parts.
     * @throws Exception if an error occurs during processing.
     */
    @GET
    @Path("getCount")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCount(@Context UriInfo uriInfo) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
        if (input == null) {
            throw new FileNotFoundException("sp.properties file not found.");
        }
        pro.load(input);

        String supplierTable = pro.getProperty("supplierpersondetailsTable");
        String caSupplierTable = pro.getProperty("casuppliersdetailsTable");
        String deviationTable = pro.getProperty("deviationTable");
        String changeactionTable = pro.getProperty("catable");
        String companyDetailsTable = pro.getProperty("companyTable");
        String mpnTable = pro.getProperty("MpnTable");
        String mpnRelatedPartsTable = pro.getProperty("MpnconnectionTable");
        String ecPartTable = pro.getProperty("ecpartTable");

        String email = uriInfo.getQueryParameters().getFirst("email");
        if (email == null || email.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty email parameter\" }";
        }

        // Queries
        String deviationQuery = String.format(
            "SELECT COUNT(*) FROM %s spd " +
            "JOIN %s casd ON spd.companyid = casd.companyid " +
            "JOIN %s dev ON dev.deviationid = casd.changenumber " +
            "WHERE spd.email_address = ?",
            supplierTable, caSupplierTable, deviationTable
        );

        String changeactionQuery = String.format(
            "SELECT COUNT(*) FROM %s spd " +
            "JOIN %s casd ON spd.companyid = casd.companyid " +
            "JOIN %s ca ON ca.caid = casd.changenumber " +
            "WHERE spd.email_address = ?",
            supplierTable, caSupplierTable, changeactionTable
        );

        String ecPartsCountQuery = String.format(
            "SELECT COUNT(*) FROM %s spd " +
            "JOIN %s cd ON spd.companyid = cd.companyid " +
            "JOIN %s mpn ON cd.name = mpn.manufacturername " +
            "JOIN %s mrp ON mpn.mpnid = mrp.mpnid " +
            "JOIN %s part ON part.partid=mrp.partid " +
            "WHERE spd.email_address = ?",
            supplierTable, companyDetailsTable, mpnTable, mpnRelatedPartsTable, ecPartTable
        );

        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection(url, userName, password);
        try {
            // Prepare statements
            PreparedStatement supplierStmt = conn.prepareStatement(deviationQuery);
            PreparedStatement caStmt = conn.prepareStatement(changeactionQuery);
            PreparedStatement ecPartsStmt = conn.prepareStatement(ecPartsCountQuery);

            supplierStmt.setString(1, email);
            caStmt.setString(1, email);
            ecPartsStmt.setString(1, email);

            // Execute queries
            ResultSet supplierResult = supplierStmt.executeQuery();
            ResultSet caResult = caStmt.executeQuery();
            ResultSet ecPartsResult = ecPartsStmt.executeQuery();

            // Initialize counts
            int deviationCount = 0;
            int changeActionCount = 0;
            int ecPartsCount = 0;

            // Get the count for deviations
            if (supplierResult.next()) {
                deviationCount = supplierResult.getInt(1);
            }

            // Get the count for change actions
            if (caResult.next()) {
                changeActionCount = caResult.getInt(1);
            }

            // Get the count for EC parts
            if (ecPartsResult.next()) {
                ecPartsCount = ecPartsResult.getInt(1);
            }

            // Build the JSON response
            String jsonResponse = String.format(
                "{ \"results\": [{ \"changeaction\": %d, \"deviation\": %d, \"ecparts\": %d }] }",
                changeActionCount, deviationCount, ecPartsCount
            );

            return jsonResponse;
        } finally {
            // Clean up resources
            if (conn != null) conn.close();
        }
    }
	
    /**
     * Retrieves the supplier name associated with the given email.
     * 
     * @param uriInfo The UriInfo context for extracting query parameters.
     * @return JSON string containing the supplier name or an error message.
     * @throws Exception if an error occurs during processing.
     */
    @GET
    @Path("getSupplierName")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSupplierName(@Context UriInfo uriInfo) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
        if (input == null) {
            throw new FileNotFoundException("sp.properties file not found.");
        }
        pro.load(input);

        String supplierPersonTable = pro.getProperty("supplierpersondetailsTable");
        String companyTable = pro.getProperty("companyTable");

        // Get email from query parameters
        String email = uriInfo.getQueryParameters().getFirst("username");
        if (email == null || email.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty username parameter\" }";
        }

        // SQL query
        String query = String.format(
            "SELECT c.name FROM %s sp JOIN %s c ON sp.companyid = c.companyid WHERE sp.email_address = ?",
            supplierPersonTable, companyTable
        );

        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection(url, userName, password);

        // Create JSON structure
        JSONObject jsonResponse = new JSONObject();
        JSONArray resultsArray = new JSONArray();

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email); // Set the email in the query
            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                String companyName = resultSet.getString("name");
                // Create JSON object for the result
                JSONObject resultObject = new JSONObject();
                resultObject.put("suppliername", companyName);
                resultsArray.put(resultObject);
            } else {
                // Handle case where no supplier was found
                JSONObject resultObject = new JSONObject();
                resultObject.put("suppliername", "No supplier found for the provided email");
                resultsArray.put(resultObject);
            }

            // Add results array to the response object
            jsonResponse.put("results", resultsArray);

            // Return the JSON response
            return jsonResponse.toString();

        } catch (SQLException e) {
            e.printStackTrace();

            // Create error response in case of exception
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", "An error occurred while fetching the supplier details");
            return errorResponse.toString();
        } finally {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
    }
	
    /**
     * Retrieves the visibility of assigned parts based on supplier name and part ID.
     *
     * @param uriInfo The UriInfo context for extracting query parameters.
     * @return JSON string indicating visibility of the part.
     * @throws Exception if an error occurs during processing.
     */
    @GET
    @Path("getAssignedPartsVisibility")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAssignedPartsVisibility(@Context UriInfo uriInfo) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
        if (input == null) {
            throw new FileNotFoundException("sp.properties file not found.");
        }
        pro.load(input);

        String mpnTable = pro.getProperty("MpnTable");
        String connectionTableName = pro.getProperty("MpnconnectionTable");

        // Get email from query parameters
        String supplierName = uriInfo.getQueryParameters().getFirst("suppliername");
        String partId = uriInfo.getQueryParameters().getFirst("partid");
        if (supplierName == null || supplierName.trim().isEmpty() || partId == null || partId.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty suppliername or partid parameter\" }";
        }

        // SQL query
        String sql = "SELECT ct.partid " +
                     "FROM " + connectionTableName + " ct " +
                     "JOIN " + mpnTable + " mt ON ct.mpnid = mt.mpnid " +
                     "WHERE ct.partid = ? " +
                     "AND mt.manufacturername = ?";

        String retrievedPartId = null;
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setString(1, partId); // Set the partid parameter
            preparedStatement.setString(2, supplierName); // Set the suppliername parameter

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    retrievedPartId = resultSet.getString("partid"); // Retrieve the partid from the result set
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle exceptions appropriately
        }

        // Create the desired JSON structure
        JSONObject jsonResponse = new JSONObject();
        JSONArray resultsArray = new JSONArray();
        JSONObject resultObject = new JSONObject();

        resultObject.put("Visibility", retrievedPartId != null ? "true" : "false");
        resultObject.put("id", retrievedPartId);

        resultsArray.put(resultObject);
        jsonResponse.put("results", resultsArray);

        return jsonResponse.toString();
    }
	
    /**
     * Searches for Change Actions based on provided criteria.
     *
     * @param jsonInput The JSON input containing search criteria.
     * @return Response object containing the search results.
     * @throws IOException if an error occurs during processing.
     */
    @POST
    @Path("searchForCA")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataCA(String jsonInput) throws IOException {
        if (jsonInput == null || jsonInput.trim().isEmpty() || !jsonInput.trim().startsWith("{")) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "fail");
            errorResponse.put("message", "Invalid JSON input.");
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(errorResponse.toString())
                           .type(MediaType.APPLICATION_JSON)
                           .build();
        }

        JSONObject json = new JSONObject(jsonInput);
        String text = json.optString("text", "%"); // Default to '%' if text is empty
        String field = json.optString("field");

        // Validate input for 'Name' field
        if (field.equalsIgnoreCase("Name")) {
            if (!text.startsWith("CA-") || text.contains("*00*")) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("status", "fail");
                errorResponse.put("message", "For 'Name' field, the text must start with 'CA-' and cannot contain '*00*'.");
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(errorResponse.toString())
                               .type(MediaType.APPLICATION_JSON)
                               .build();
            }
        }

        // Ensure the search text contains at least 3 characters
        if (text.equals("*") || text.length() < 3) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "fail");
            errorResponse.put("message", "Please provide at least 3 characters or digits for the search.");
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(errorResponse.toString())
                           .type(MediaType.APPLICATION_JSON)
                           .build();
        }

        // Handle 'Everything' field - special case for caid and name
        if (field.equalsIgnoreCase("Everything")) {
            if (text.matches("\\d{5}\\.\\d{5}\\.\\d{5}\\.\\d{4}")) {
                // If text matches exact caid format, use an exact match
                text = text;
            } else if (text.matches("\\d{5}\\.\\d{5}\\.\\d{5}\\.*")) {
                // If it's a partial caid like 24724.43239.18190.*, return empty results
                JSONObject js = new JSONObject();
                js.put("results", new JSONArray());
                return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
            } else if (text.startsWith("CA-")) {
                // If the text starts with CA-, apply wildcard search
                text = text.replace("*", "%");
            } else {
                // If the input is invalid for Everything, return empty results
                JSONObject js = new JSONObject();
                js.put("results", new JSONArray());
                return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
            }
        } else {
            // For other fields, use LIKE with wildcards
            text = text.replace("*", "%");
            if (!text.startsWith("%")) {
                text = "%" + text;
            }
            if (!text.endsWith("%")) {
                text = text + "%";
            }
        }

        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        JSONArray jsonArray = new JSONArray();

        try {
            Class.forName("org.postgresql.Driver");
            Connection con = DriverManager.getConnection(url, userName, password);
            String sql = "";

            if (field.equalsIgnoreCase("Everything")) {
                // Exact caid match or partial search for 'name' starting with CA-
                sql = "SELECT caid, name, type FROM changeaction WHERE (caid = ? OR name ILIKE ?) AND name ILIKE 'CA-%'";
            } else if (field.equalsIgnoreCase("name")) {
                // Search by 'name' field, only names starting with 'CA-'
                sql = "SELECT caid, name, type FROM changeaction WHERE name ILIKE ? AND name ILIKE 'CA-%'";
            }

            if (!sql.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    if (field.equalsIgnoreCase("Everything")) {
                        ps.setString(1, text); // Use exact 'caid' or processed 'name' search
                        ps.setString(2, text); // For CA- based name
                    } else {
                        ps.setString(1, text); // Use processed text for 'name' search
                    }

                    ResultSet set = ps.executeQuery();
                    Map<String, List<String>> caidsByType = new HashMap<>();

                    while (set.next()) {
                        String caid = set.getString("caid");
                        String type = set.getString("type");

                        // Check if the caid exists in ca_suppliers_details with acknowledge = 'Yes' or 'No'
                        String checkSql = "SELECT COUNT(*) FROM ca_suppliers_details WHERE changenumber = ? AND (acknowledge = 'Yes' OR acknowledge = 'No')";
                        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                            checkPs.setString(1, caid);
                            ResultSet checkSet = checkPs.executeQuery();
                            if (checkSet.next() && checkSet.getInt(1) > 0) {
                                // If the caid exists and acknowledge is 'Yes' or 'No', add it to the list
                                caidsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(caid);
                            }
                        }
                    }

                    // Construct JSON output
                    for (Map.Entry<String, List<String>> entry : caidsByType.entrySet()) {
                        String type = entry.getKey();
                        List<String> caids = entry.getValue();

                        JSONObject jsonObject = new JSONObject();
                        JSONObject typeObject = new JSONObject();
                        typeObject.put("caid", String.join("|", caids));
                        jsonObject.put("type: " + type, typeObject);
                        jsonArray.put(jsonObject);
                    }
                }
            }

            JSONObject js = new JSONObject();
            js.put("results", jsonArray);
            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject js = new JSONObject();
            js.put("status", "fail");
            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Searches for deviations based on the provided criteria.
     *
     * @param s The JSON input string containing search criteria.
     * @return Response object containing the search results.
     * @throws IOException if an error occurs during processing.
     */
    @POST
    @Path("searchForDeviation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataDev(String s) throws IOException {
        // Validate input JSON string
        if (s == null || s.trim().isEmpty() || !s.trim().startsWith("{")) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "fail");
            errorResponse.put("message", "Invalid JSON input.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse.toString())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Parse input JSON and extract search text and field
        JSONObject json = new JSONObject(s);
        String text = json.optString("text", "%"); // Default to '%' if text is empty
        String field = json.optString("field");

        // Validate input for 'Name' field
        if (field.equalsIgnoreCase("Name")) {
            if (!text.startsWith("DEV-") || text.contains("*00*")) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("status", "fail");
                errorResponse.put("message", "For 'Name' field, the text must start with 'DEV-' and cannot contain '*00*'.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse.toString())
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
        }

        // Ensure the search text contains at least 3 characters
        if (text.equals("*") || text.length() < 3) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "fail");
            errorResponse.put("message", "Please provide at least 3 characters or digits for the search.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse.toString())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Handle 'Everything' field with special cases for deviationid and name
        if (field.equalsIgnoreCase("Everything")) {
            if (text.matches("\\d{5}\\.\\d{5}\\.\\d{5}\\.\\d{4}")) {
                // Exact match for deviationid format
                text = text;
            } else if (text.matches("\\d{5}\\.\\d{5}\\.\\d{5}\\.*")) {
                // Partial deviationid like 24724.43239.18190.*
                return createEmptyResultsResponse();
            } else if (text.startsWith("DEV-")) {
                // Wildcard search for DEV- based names
                text = text.replace("*", "%");
            } else {
                // Invalid input for Everything field
                return createEmptyResultsResponse();
            }
        } else {
            // Use LIKE with wildcards for other fields
            text = text.replace("*", "%");
            if (!text.startsWith("%")) {
                text = "%" + text;
            }
            if (!text.endsWith("%")) {
                text = text + "%";
            }
        }

        // Database connection setup
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        JSONArray jsonArray = new JSONArray();

        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish connection
            try (Connection con = DriverManager.getConnection(url, userName, password)) {
                String sql = buildSQLQuery(field);

                if (!sql.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        // Set parameters for query execution
                        if (field.equalsIgnoreCase("Everything")) {
                            ps.setString(1, text);  // Exact deviationid or processed name
                            ps.setString(2, text);  // For DEV- based name
                        } else {
                            ps.setString(1, text);  // Processed text for 'name' search
                        }

                        ResultSet set = ps.executeQuery();
                        Map<String, List<String>> deviationidsByType = new HashMap<>();

                        // Process the result set
                        while (set.next()) {
                            String deviationid = set.getString("deviationid");
                            String type = set.getString("type");

                            // Check if the deviationid exists in ca_suppliers_details with acknowledge = 'Yes' or 'No'
                            String checkSql = "SELECT COUNT(*) FROM ca_suppliers_details WHERE changenumber = ? AND (acknowledge = 'Yes' OR acknowledge = 'No')";
                            try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                                checkPs.setString(1, deviationid);
                                ResultSet checkSet = checkPs.executeQuery();
                                if (checkSet.next() && checkSet.getInt(1) > 0) {
                                    // Add deviationid to list if acknowledge is 'Yes' or 'No'
                                    deviationidsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(deviationid);
                                }
                            }
                        }

                        // Construct JSON output
                        for (Map.Entry<String, List<String>> entry : deviationidsByType.entrySet()) {
                            String type = entry.getKey();
                            List<String> deviationids = entry.getValue();

                            JSONObject jsonObject = new JSONObject();
                            JSONObject typeObject = new JSONObject();
                            typeObject.put("deviationid", String.join("|", deviationids));
                            jsonObject.put("type: " + type, typeObject);
                            jsonArray.put(jsonObject);
                        }
                    }
                }
            }

            // Return final results as JSON response
            JSONObject js = new JSONObject();
            js.put("results", jsonArray);
            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            // Debugging output for exception handling
            e.printStackTrace();
            JSONObject js = new JSONObject();
            js.put("status", "fail");
            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Builds the SQL query string based on the provided search field.
     *
     * @param field The search field for which the query is constructed.
     * @return SQL query string.
     */
    private String buildSQLQuery(String field) {
        if (field.equalsIgnoreCase("Everything")) {
            // Exact deviationid match or partial search for 'name' starting with DEV-
            return "SELECT deviationid, name, type FROM deviation_details WHERE (deviationid = ? OR name ILIKE ?) AND name ILIKE 'DEV-%'";
        } else if (field.equalsIgnoreCase("name")) {
            // Search by 'name' field, only names starting with 'DEV-'
            return "SELECT deviationid, name, type FROM deviation_details WHERE name ILIKE ? AND name ILIKE 'DEV-%'";
        }
        return "";
    }

    /**
     * Creates an empty results response.
     *
     * @return Response with empty JSON results.
     */
    private Response createEmptyResultsResponse() {
        JSONObject js = new JSONObject();
        js.put("results", new JSONArray());
        return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
    }
	
    /**
     * Checks the supplier user for a given part ID and retrieves related details.
     *
     * @param uriInfo The URI information containing the query parameters.
     * @return A JSON string containing the supplier details or an error message.
     * @throws Exception if an error occurs during processing.
     */
    @GET
    @Path("getSupplierusercheckforebom")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSupplierUserCheckForEBOM(@Context UriInfo uriInfo) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file (if necessary)
        Properties pro = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
        if (input == null) {
            throw new FileNotFoundException("sp.properties file not found.");
        }
        pro.load(input);

        // Get partId from query parameters
        String partId = uriInfo.getQueryParameters().getFirst("partid");
        if (partId == null || partId.trim().isEmpty()) {
            return "{ \"error\": \"Missing or empty partId parameter\" }";
        }

        // Define SQL queries
        String nameAndManufacturerQuery = "SELECT cd.name, mpn.manufacturername " +
                                           "FROM mpn_related_parts_details mrpd " +
                                           "JOIN mpn ON mrpd.mpnid = mpn.mpnid " +
                                           "JOIN company_details cd ON mpn.manufacturername = cd.name " +
                                           "JOIN supplier_person_details spd ON cd.companyid = spd.companyid " +
                                           "WHERE mrpd.partid = ?";

        String moduleEndItemQuery = "SELECT ec.module_end_item " +
                                     "FROM ec_parts_details ec " +
                                     "WHERE ec.partid = ?";

        Class.forName("org.postgresql.Driver");

        // Initialize response variables
        String cdName = "";
        String manufacturerName = "";
        String moduleEndItem = "";
        boolean isMatch = false;

        // Connect to the database and execute the queries
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             PreparedStatement nameAndManufacturerStmt = conn.prepareStatement(nameAndManufacturerQuery);
             PreparedStatement moduleEndItemStmt = conn.prepareStatement(moduleEndItemQuery)) {

            // Set the partId parameter in the first query
            nameAndManufacturerStmt.setString(1, partId);
            ResultSet rs = nameAndManufacturerStmt.executeQuery();

            // Process the result set for cd.name and mpn.manufacturername
            if (rs.next()) {
                cdName = rs.getString("name");
                manufacturerName = rs.getString("manufacturername");

                // Check if cd.name equals mpn.manufacturername
                isMatch = cdName.equals(manufacturerName);
            }

            // Set the partId parameter in the second query
            moduleEndItemStmt.setString(1, partId);
            ResultSet rsModule = moduleEndItemStmt.executeQuery();

            // Process the result set for module_end_item
            if (rsModule.next()) {
                moduleEndItem = rsModule.getString("module_end_item");
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Retain for debugging
            return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
        }

        // Create JSON response
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("name in companydetails", cdName);
        jsonResponse.put("manufacturername in mpn", manufacturerName);
        jsonResponse.put("isMatch", isMatch);
        jsonResponse.put("module_end_item", moduleEndItem); // Add the module_end_item attribute

        return jsonResponse.toString();
    }
}

