package Sp.Supplierportal;




import java.io.*;
import java.sql.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("portaldata")
public class PortalData {
	
	
	@GET
	@Path("selectattributes")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSelectAttributes(@QueryParam("selectedAttributes") String selectedAttributes,
	                                  @QueryParam("partid") String partid) throws Exception {
		String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String user= System.getenv("SupplierPortalDBUsername");
		
	    Properties properties = new Properties();
	    StringBuilder allValues = new StringBuilder();
	    JSONObject responseObject = new JSONObject();

	    try {
	        InputStream propertiesData = getClass().getClassLoader().getResourceAsStream("Portal.properties");
	        if (propertiesData == null) {
	            throw new FileNotFoundException("Portal.properties not found.");
	        }
	        properties.load(propertiesData);

	        // Ensure PostgreSQL Driver is loaded
	        Class.forName("org.postgresql.Driver");

	        String mpnTableName = properties.getProperty("MPNDetailsTable");
	        String relTableName = properties.getProperty("ConnectionTable");
	        String attributes = properties.getProperty("Attribute_MPN_All");
	        String[] attributeValues = attributes.split(",");
	        Map<String, String> attributeMap = new HashMap<>();
	        Map<String, String> displayNamesMap = new HashMap<>();

	        for (String attr : attributeValues) {
	            String[] attrValues = attr.split("\\|");
	            String key = attrValues[0].trim();
	            String value = attrValues[1].trim();
	            attributeMap.put(key, value);
	            String displayName = properties.getProperty("Attribute_" + key);
	            if (displayName != null) {
	                displayNamesMap.put(value, displayName);
	            }
	        }

	        if (selectedAttributes != null && !selectedAttributes.isEmpty()) {
	            String[] selected = selectedAttributes.split(",");

	            for (String selectedAttr : selected) {
	                String trimmedAttr = selectedAttr.trim();
	                if (attributeMap.containsKey(trimmedAttr)) {
	                    if (allValues.length() > 0) {
	                        allValues.append(", ");
	                    }
	                    allValues.append(attributeMap.get(trimmedAttr));
	                }
	            }
	        }

	        String allAttrValues = allValues.toString();
	        String mpnidSql = "SELECT mpnid FROM " + relTableName + " WHERE partid = ?";
	        String sql = "SELECT " + allAttrValues + " FROM " + mpnTableName + " WHERE mpnid = ?";
	        try (Connection conn = DriverManager.getConnection(url, user, password);
	             PreparedStatement mpnidStmt = conn.prepareStatement(mpnidSql)) {
	            mpnidStmt.setString(1, partid);
	            ResultSet mpnidResult = mpnidStmt.executeQuery();
	            if (mpnidResult.next()) {
	                String mpnid = mpnidResult.getString("mpnid");

	                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
	                    stmt.setString(1, mpnid);

	                    ResultSet result = stmt.executeQuery();
	                    ResultSetMetaData metaData = result.getMetaData();
	                    int columnCount = metaData.getColumnCount();

	                    JSONArray objectDetailsArray = new JSONArray();

	                    while (result.next()) {
	                        // Keep track of selectable and normal attributes
	                        JSONArray selectablesArray = new JSONArray();
	                        JSONArray attributesArray = new JSONArray();

	                        for (int i = 1; i <= columnCount; i++) {
	                            String columnName = metaData.getColumnName(i);
	                            String columnValue = result.getString(i);

	                            // Skip the `id` from being added to selectables or attributes
	                            if (columnName.equalsIgnoreCase("id") || columnName.equalsIgnoreCase("mpnid")) {
	                                continue;
	                            }

	                            JSONObject attrObject = new JSONObject();
	                            String displayName = displayNamesMap.get(columnName);
	                            if (displayName == null) {
	                                displayName = columnName;
	                            }
	                            attrObject.put("name", columnName);
	                            attrObject.put("value", columnValue);
	                            attrObject.put("displayname", displayName);

	                            if (isSelectable(columnName)) {
	                                selectablesArray.put(attrObject);
	                            } else {
	                                attributesArray.put(attrObject);
	                            }
	                        }

	                        JSONObject objectDetails = new JSONObject();
	                        objectDetails.put("selectables", selectablesArray);
	                        objectDetails.put("attributes", attributesArray);
	                        objectDetails.put("id", mpnid);  // Keep mpnid in the output

	                        objectDetailsArray.put(objectDetails);
	                    }

	                    responseObject.put("objectdetails", objectDetailsArray);
	                }
	            } else {
	                throw new RuntimeException("No mpnid found for the provided partid.");
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new RuntimeException("Error fetching selected attributes", e);
	    }
	    return responseObject.toString();
	}



	/**
	 * This method provides a list of all MPN attributes.
	 * Used for Supplier Portal purposes.
	 *
	 * <p>Example Input:</p>
	 * <pre>{@code
	 * {
	 *   "partid": "123456"
	 * }
	 * }</pre>
	 *
	 * @param partid The Part ID.
	 * @return A JSON string containing all MPN attributes.
	 * @throws SQLException if an error occurs during database access.
	 */
	@GET
	@Path("mpnallattributes")
	@Produces(MediaType.APPLICATION_JSON)
	public String getMpnAllAttributes(@QueryParam("partid") String partid) {
		String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String user= System.getenv("SupplierPortalDBUsername");
		
	    Properties properties = new Properties();
	    JSONObject responseObject = new JSONObject();

	    try {
	        InputStream propertiesData = getClass().getClassLoader().getResourceAsStream("Portal.properties");
	        if (propertiesData == null) {
	            throw new FileNotFoundException("Portal.properties not found.");
	        }
	        properties.load(propertiesData);

	        Class.forName("org.postgresql.Driver");

	        String mpnTableName = properties.getProperty("MPNDetailsTable");
	        String relTableName = properties.getProperty("ConnectionTable");
	        String attributes = properties.getProperty("Attribute_MPN_All");
	        String[] attributeValues = attributes.split(",");

	        Map<String, String> displayNamesMap = new HashMap<>();
	        StringBuilder allAttributeValuesBuilder = new StringBuilder();

	        for (String attr : attributeValues) {
	            String[] attrValues = attr.split("\\|");
	            String key = attrValues[0].trim();
	            String value = attrValues[1].trim();
	            String displayName = properties.getProperty("Attribute_" + key);
	            if (displayName != null) {
	                displayNamesMap.put(value, displayName);
	            } else {
	                displayNamesMap.put(value, key);
	            }
	            if (allAttributeValuesBuilder.length() > 0) {
	                allAttributeValuesBuilder.append(", ");
	            }
	            allAttributeValuesBuilder.append(value);
	        }
	        String allAttrValues = allAttributeValuesBuilder.toString();

	        // Prepare SQL queries
	        String mpnidSql = null;
	        String sql = "SELECT " + allAttrValues + " FROM " + mpnTableName;
	        if (partid != null && !partid.isEmpty()) {
	            mpnidSql = "SELECT mpnid FROM " + relTableName + " WHERE partid = ?";
	            sql += " WHERE mpnid = ?";
	        }

	        // Execute queries and build response
	        try (Connection conn = DriverManager.getConnection(url, user, password);
	             PreparedStatement mpnidStmt = (mpnidSql != null) ? conn.prepareStatement(mpnidSql) : null;
	             PreparedStatement stmt = conn.prepareStatement(sql)) {

	            if (mpnidStmt != null) {
	                mpnidStmt.setString(1, partid);
	                try (ResultSet mpnidResult = mpnidStmt.executeQuery()) {
	                    if (mpnidResult.next()) {
	                        String mpnid = mpnidResult.getString("mpnid");
	                        stmt.setString(1, mpnid);
	                    } else {
	                        throw new RuntimeException("No mpnid found for the provided partid.");
	                    }
	                }
	            }

	            try (ResultSet result = stmt.executeQuery()) {
	                ResultSetMetaData metaData = result.getMetaData();
	                int columnCount = metaData.getColumnCount();
	                JSONArray objectDetailsArray = new JSONArray();

	                while (result.next()) {
	                    String id = result.getString("mpnid");
	                    JSONArray selectablesArray = new JSONArray();
	                    JSONArray attributesArray = new JSONArray();

	                    for (int i = 1; i <= columnCount; i++) {
	                        String columnName = metaData.getColumnName(i);
	                        String columnValue = result.getString(i);

	                        // Skip adding 'id' or 'mpnid' to selectables or attributes arrays
	                        if (columnName.equalsIgnoreCase("id") || columnName.equalsIgnoreCase("mpnid")) {
	                            continue;
	                        }

	                        JSONObject attrObject = new JSONObject();
	                        String displayName = displayNamesMap.get(columnName);
	                        if (displayName == null) {
	                            displayName = columnName;
	                        }
	                        attrObject.put("name", columnName);
	                        attrObject.put("value", columnValue);
	                        attrObject.put("displayname", displayName);

	                        if (isSelectable(columnName)) {
	                            selectablesArray.put(attrObject);
	                        } else {
	                            attributesArray.put(attrObject);
	                        }
	                    }

	                    JSONObject objectDetails = new JSONObject();
	                    objectDetails.put("selectables", selectablesArray);
	                    objectDetails.put("attributes", attributesArray);
	                    objectDetails.put("id", id);  // Keep id/mnpid in the response

	                    objectDetailsArray.put(objectDetails);
	                }

	                responseObject.put("objectdetails", objectDetailsArray);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new RuntimeException("Error fetching attributes", e);
	    }
	    return responseObject.toString();
	}





	/**
	 * Determines if a given column name is considered selectable based on predefined criteria.
	 * The criteria are typically based on column names that are significant for selection purposes
	 * within the application.
	 *
	 * <p>Example Usage:</p>
	 * <pre>{@code
	 * boolean isTypeSelectable = isSelectable("Type"); // returns true
	 * boolean isDateSelectable = isSelectable("Date"); // returns false
	 * }</pre>
	 *
	 * @param columnName The name of the column to be evaluated.
	 * @return {@code true} if the column name matches one of the predefined selectable criteria;
	 *         {@code false} otherwise.
	 */


    // Define your criteria for distinguishing Selectables
    public boolean isSelectable(String columnName) {
        // Example criteria, customize based on your needs
        return columnName.equalsIgnoreCase("Type") ||
               columnName.equalsIgnoreCase("Name") ||
               columnName.equalsIgnoreCase("Revision") ||
               columnName.equalsIgnoreCase("Description") || 
               columnName.equalsIgnoreCase("mpnid") ||
               columnName.equalsIgnoreCase("policy") ||
               columnName.equalsIgnoreCase("state") ||
               columnName.equalsIgnoreCase("project")||
               columnName.equalsIgnoreCase("organization")||
               columnName.equalsIgnoreCase("owner");
    }
    
    /**
     * This method provides a list of part and MPN details referring to the `MPN_Related_Parts_Details` table.
     * Used for Supplier Portal purposes.
     *
     * <p>Example Input:</p>
     * <pre>{@code
     * 
     *   "partid": "987654",
     *   "connection": true,
     *   "selectedAttributes": "attribute1", "attribute2", "attribute3"
     * 
     * }</pre>
     *
     * @param partId The part ID.
     * @param selectedAttributes A comma-separated list of selected attributes.
     * @return A JSON string containing the part and MPN details.
     * @throws Exception if an error occurs during processing.
     */

    @GET
    @Path("connectiondetails")
    @Produces(MediaType.APPLICATION_JSON)
    public String getConnectionDetails(@QueryParam("PartId") String partId, 
                                       @QueryParam("selectedAttributes") String selectedAttributes,
                                       @QueryParam("connection") boolean connection) throws Exception {
    	String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String user= System.getenv("SupplierPortalDBUsername");
        Properties properties = new Properties();
        JSONObject responseObject = new JSONObject();

        try {
            InputStream propertiesData = getClass().getClassLoader().getResourceAsStream("Portal.properties");

            if (propertiesData == null) {
                throw new FileNotFoundException("Portal.properties not found.");
            }
            properties.load(propertiesData);
            Class.forName("org.postgresql.Driver");

            String mpnDetailsTable = properties.getProperty("MPNDetailsTable");
            String partTable = properties.getProperty("PartTable");
            String connectionTable = properties.getProperty("ConnectionTable");
            String mpnAttributes = properties.getProperty("Attribute_MPN_All");
            String partAttributes = properties.getProperty("Part_Attributes");
            String connectionAttributes = properties.getProperty("Related_Attributes");
            String relid = "";
            Map<String, String> partDisplayNamesMap = new HashMap<>();
            Map<String, String> mpnDisplayNamesMap = new HashMap<>();
            Map<String, String> mpnAttributeMap = new HashMap<>();
            Map<String, String> displayNamesMap = new HashMap<>();
            Map<String, String> partAttributeMap = new HashMap<>();
            Map<String, String> connectionAttributeMap = new HashMap<>();

            String[] mpnAttributeValues = mpnAttributes.split(",");
            for (String attr : mpnAttributeValues) {
                String[] attrValues = attr.split("\\|");
                String key = attrValues[0].trim();
                String value = attrValues[1].trim();
                mpnAttributeMap.put(key, value);

                String displayName = properties.getProperty("Attribute_" + key);
                if (displayName != null) {
                    mpnDisplayNamesMap.put(value, displayName); 
                }
            }

            String[] partAttributeValues = partAttributes.split(",");
            for (String attr : partAttributeValues) {
                String[] attrValues = attr.split("\\|");
                String key = attrValues[0].trim();
                String value = attrValues[1].trim(); 
                partAttributeMap.put(key, value);

//                String normalizedKey = key.replaceAll(" ", "_"); 
                String displayName = properties.getProperty("Attribute_" + key);
                if (displayName != null) {
                    partDisplayNamesMap.put(value, displayName); 
                }
            }

            String[] connectionAttributeValues = connectionAttributes.split(",");
            for (String attr : connectionAttributeValues) {
                String[] attrValues = attr.split("\\|");
                String key = attrValues[0].trim();
                String value = attrValues[1].trim();
                connectionAttributeMap.put(key, value);
                String displayName = properties.getProperty("Attribute_" + key);
                if (displayName != null) {
                    displayNamesMap.put(value, displayName);
                } else {
                    displayNamesMap.put(value, key);
                }
            }

            StringBuilder selectedMpnAttributesBuilder = new StringBuilder();
            StringBuilder selectedPartAttributesBuilder = new StringBuilder();

            if (selectedAttributes != null && !selectedAttributes.isEmpty()) {
                String[] selected = selectedAttributes.split(",");
                for (String selectedAttr : selected) {
                    String trimmedAttr = selectedAttr.trim();
                    if (mpnAttributeMap.containsKey(trimmedAttr)) {
                        if (selectedMpnAttributesBuilder.length() > 0) {
                            selectedMpnAttributesBuilder.append(", ");
                        }
                        selectedMpnAttributesBuilder.append(mpnAttributeMap.get(trimmedAttr));
                    }
                    if (partAttributeMap.containsKey(trimmedAttr)) {
                        if (selectedPartAttributesBuilder.length() > 0) {
                            selectedPartAttributesBuilder.append(", ");
                        }
                        selectedPartAttributesBuilder.append(partAttributeMap.get(trimmedAttr));
                    }
                }
            } else {
                selectedMpnAttributesBuilder.append(String.join(", ", mpnAttributeMap.values()));
                selectedPartAttributesBuilder.append(String.join(", ", partAttributeMap.values()));
            }

            String selectedMpnAttributes = selectedMpnAttributesBuilder.toString();
            String selectedPartAttributes = selectedPartAttributesBuilder.toString();

            Connection conn = DriverManager.getConnection(url, user, password);

            String joinQuery = "SELECT c.partid AS p_partid, c.mpnid AS m_mpnid " +
                               (connection ? ", " + String.join(", ", connectionAttributeMap.values()) : "") +
                               " FROM " + connectionTable + " c WHERE c.partid = ?";
            PreparedStatement joinStmt = conn.prepareStatement(joinQuery);
            joinStmt.setString(1, partId);
            ResultSet joinResultSet = joinStmt.executeQuery();

            JSONArray objectDetailsArray = new JSONArray();

            while (joinResultSet.next()) {
                String partIdValue = joinResultSet.getString("p_partid");
                String mpnIdValue = joinResultSet.getString("m_mpnid");
                relid = joinResultSet.getString("relid");
                JSONArray partAttributesArray = new JSONArray();
                JSONArray mpnAttributesArray = new JSONArray();
                JSONObject connectionAttributesObject = new JSONObject();

                String partQuery = "SELECT " + selectedPartAttributes + " FROM " + partTable + " WHERE partid = ?";
                PreparedStatement partStmt = conn.prepareStatement(partQuery);
                partStmt.setString(1, partIdValue);
                ResultSet partResultSet = partStmt.executeQuery();

                ResultSetMetaData partMetaData = partResultSet.getMetaData();
                int partColumnCount = partMetaData.getColumnCount();

                while (partResultSet.next()) {
                    for (int i = 1; i <= partColumnCount; i++) {
                        String columnName = partMetaData.getColumnName(i);
                        String columnValue = partResultSet.getString(i);
                        if (columnName.equalsIgnoreCase("id") || columnName.equalsIgnoreCase("mpnid")) {
                            continue;
                        }

                        JSONObject attrObject = new JSONObject();
                        String displayName = partDisplayNamesMap.get(columnName);
                        if (displayName == null) {
                            displayName = columnName;
                        }
                        attrObject.put("name", columnName);
                        attrObject.put("value", columnValue);
                        attrObject.put("displayname", displayName);

                        partAttributesArray.put(attrObject);
                    }
                }

                String mpnQuery = "SELECT " + selectedMpnAttributes + " FROM " + mpnDetailsTable + " WHERE mpnid = ?";
                PreparedStatement mpnStmt = conn.prepareStatement(mpnQuery);
                mpnStmt.setString(1, mpnIdValue);
                ResultSet mpnResultSet = mpnStmt.executeQuery();

                ResultSetMetaData mpnMetaData = mpnResultSet.getMetaData();
                int mpnColumnCount = mpnMetaData.getColumnCount();

                while (mpnResultSet.next()) {
                	
                    for (int i = 1; i <= mpnColumnCount; i++) {
                        String columnName = mpnMetaData.getColumnName(i);
                        String columnValue = mpnResultSet.getString(i);
                        if (columnName.equalsIgnoreCase("id") || columnName.equalsIgnoreCase("partid")) {
                            continue;
                        }
                        JSONObject attrObject = new JSONObject();
                        String displayName = mpnDisplayNamesMap.get(columnName);
                        if (displayName == null) {
                            displayName = columnName;
                        }
                        attrObject.put("name", columnName);
                        attrObject.put("value", columnValue);
                        attrObject.put("displayname", displayName);

                        mpnAttributesArray.put(attrObject);
                    }
                }

                if (connection) {
                    for (Map.Entry<String, String> entry : connectionAttributeMap.entrySet()) {
                    	
                        String columnName = entry.getValue();
                        String columnValue = joinResultSet.getString(columnName);
                        JSONObject attrObject = new JSONObject();
                        attrObject.put("name", columnName);
                        attrObject.put("value", columnValue);
                        if (columnName.equalsIgnoreCase("mpnid") || columnName.equalsIgnoreCase("partid") ||  columnName.equalsIgnoreCase("relid")) {
                            continue;
                        }
                        String displayName = displayNamesMap.get(columnName);
                        if (displayName == null) {
                            displayName = columnName;
                        }
                        attrObject.put("displayname", columnName); 
                        connectionAttributesObject.put(columnName, attrObject);
                    }
                }

                JSONObject combinedObjectDetails = new JSONObject();
                combinedObjectDetails.put("partId", partIdValue);
                combinedObjectDetails.put("id", mpnIdValue);
                combinedObjectDetails.put("reid",relid);
                combinedObjectDetails.put("partAttributes", partAttributesArray);
                combinedObjectDetails.put("mpnAttributes", mpnAttributesArray);

                if (connection) {
                    combinedObjectDetails.put("connectionAttributes", connectionAttributesObject);
                }

                objectDetailsArray.put(combinedObjectDetails);
            }

            responseObject.put("objectdetails", objectDetailsArray);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching connection details", e);
        }
        return responseObject.toString();
    }




    /**
     * This method provides a list of supplier details.
     * Used for Supplier Portal purposes.
     *
     * <p>Example Input:</p>
     * <pre>{@code
     * {
     *   "PartId": "987654"
     * }
     * }</pre>
     *
     * @param partId The part ID.
     * @return A JSON string containing the supplier details.
     * @throws Exception if an error occurs during processing.
     */
    @GET
    @Path("supplierdetails")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSupplierDetails(@QueryParam("PartId") String partId) throws Exception {
    	String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String user= System.getenv("SupplierPortalDBUsername");
		
        Properties properties = new Properties();
        JSONObject objectDetails = new JSONObject();
        JSONArray AttributesArray = new JSONArray();

        try {
            InputStream propertiesData = getClass().getClassLoader().getResourceAsStream("Portal.properties");

            if (propertiesData == null) {
                throw new FileNotFoundException("Portal.properties not found.");
            }
            properties.load(propertiesData);
            Class.forName("org.postgresql.Driver");

            String companyTable = properties.getProperty("CompanyTable");
            String changeActionTable = properties.getProperty("CATable");
            String caDevSupplierDetails = properties.getProperty("CA_Dev_Supplierdata_Table");
            String caDevSupplierAttr = properties.getProperty("Attributes_CA_Dev_SupplierDetails");

            Map<String, String> displayNamesMap = new HashMap<>();
            Map<String, String> attributeMap = new HashMap<>();
            StringBuilder attributeValuesBuilder = new StringBuilder();
            String[] attributeValues = caDevSupplierAttr.split(",");
            for (String attr : attributeValues) {
                String[] attrValues = attr.split("\\|");
                String key = attrValues[0].trim();
                String value = attrValues[1].trim();
                attributeMap.put(key, value);

                // Load display names from properties
                
                String displayName = properties.getProperty("Attribute_" + key);
                if (displayName != null) {
                    displayNamesMap.put(value, displayName);
                } else {
                    displayNamesMap.put(value, key);
                }

                if (attributeValuesBuilder.length() > 0) {
                    attributeValuesBuilder.append(", ");
                }
                attributeValuesBuilder.append("c.").append(value);
            }
            String attrValues = attributeValuesBuilder.toString();

            // Prepare the SQL query with table aliases
            String sql = "SELECT " + attrValues + " FROM " + caDevSupplierDetails + " AS c "
                       + "JOIN " + changeActionTable + " AS a ON c.ChangeNumber = a.ChangeNumber "
                       + "JOIN " + companyTable + " AS co ON c.CompanyId = co.CompanyId "
                       + "WHERE Supplier_Visibility ='All' AND Supplier_Spec_Visibility = ?";

           

            // Establish a database connection
            Connection connection = DriverManager.getConnection(url, user, password);
                 PreparedStatement statement = connection.prepareStatement(sql);

                // Set the partId parameter
                statement.setString(1, partId);
                ResultSet resultSet = statement.executeQuery();
                ResultSetMetaData metaData = resultSet.getMetaData();
	            int columnCount = metaData.getColumnCount();
                // Execute the query
                while (resultSet.next()) {
                	String supppliername = resultSet.getString("name");
                	String companyid = resultSet.getString("companyid");
                	String changenumber = resultSet.getString("changenumber");
                	 for (int i = 1; i <= columnCount; i++) {
                		 String columnName = metaData.getColumnName(i);
  	                    String columnValue = resultSet.getString(i);
  	                 
  	                  JSONObject attrObject = new JSONObject();
  	                if (columnName.equalsIgnoreCase("companyid") || columnName.equalsIgnoreCase("changenumber")) {
                        continue;
                    }
	                    String displayName = displayNamesMap.get(columnName);
	                   
	                    if (displayName == null) {
	                        displayName = columnName; 
	                    }
	                    attrObject.put("name", columnName);
	                    attrObject.put("value", columnValue);
	                    
	                    attrObject.put("displayname",displayName);
	                    AttributesArray.put(attrObject);
                	 }
                	 	objectDetails.put("suppliername", supppliername);
		                objectDetails.put("attributes", AttributesArray);
		                objectDetails.put("companyid",companyid);
		                objectDetails.put("caid",changenumber);
                }
               
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching supplier details", e);
        }
        return objectDetails.toString();
    }
    
    
    /**
	 * This method provides a list of Devation details.
	 * Used for Supplier Portal purposes.
	 *
	 * <p>Example Input:</p>
	 * <pre>{@code
	 * {
	 *   "partid": "123456"
	 * }
	 * }</pre>
	 *
	 * @param partid The Part ID.
	 * @return A JSON string containing Deviation details.
	 * @throws SQLException,IOException if an error occurs during database access and other errors.
	 */
    @GET
    @Path("deviationdetails")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDeviationDetails(@QueryParam("partid") String partid) throws Exception {
    	String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String user= System.getenv("SupplierPortalDBUsername");
		
        Properties properties = new Properties();
        JSONObject responseObject = new JSONObject();

        try {
            InputStream propertiesData = getClass().getClassLoader().getResourceAsStream("Portal.properties");

            if (propertiesData == null) {
                throw new FileNotFoundException("Portal.properties not found.");
            }

            properties.load(propertiesData);
            Class.forName("org.postgresql.Driver");

            String DeviationTable = properties.getProperty("Deviation_Table");
            
            String DeviationRelTable = properties.getProperty("Deviation_RelTable");
            String RelAttributes = properties.getProperty("Attributes_Dev_reported");
            String DevAttributes = properties.getProperty("Attributes_Deviation");

            String[] attrValues = DevAttributes.split(",");
            String[] relAttrValues = RelAttributes.split(",");

            StringBuilder devattributeValues = new StringBuilder();
            StringBuilder relattributeValues = new StringBuilder();

            Map<String, String> displayNamesMap = new HashMap<>();

            // Map deviation attributes
            for (String attrValue : attrValues) {
                String[] attributevalue = attrValue.split("\\|");
                String key = attributevalue[0].trim();
                String value = attributevalue[1].trim();
                String displayName = properties.getProperty("Attribute_" + key);
                displayNamesMap.put(value, displayName != null ? displayName : key);
                if (devattributeValues.length() > 0) {
                    devattributeValues.append(", ");
                }
                devattributeValues.append(value);
            }

            // Map relattributes
            for (String relAttrValue : relAttrValues) {
                String[] attributevalue = relAttrValue.split("\\|");
                String key = attributevalue[0].trim();
                String value = attributevalue[1].trim();
                String displayName = properties.getProperty("Attribute_" + key);
                displayNamesMap.put(value, displayName != null ? displayName : key);
                if (relattributeValues.length() > 0) {
                    relattributeValues.append(", ");
                }
                relattributeValues.append(value);
            }

            String devattributes = devattributeValues.toString();
            String relattributes = relattributeValues.toString();

            // SQL Queries
            String sql = "SELECT " + devattributes + " FROM " + DeviationTable;
            String relSql = "SELECT " + relattributes + " FROM " + DeviationRelTable + " WHERE deviationid = ?";
            String devSql = null;

            if (partid != null && !partid.isEmpty()) {
                devSql = "SELECT deviationid FROM " + DeviationRelTable + " WHERE id = ?";
                sql += " WHERE deviationid = ?";
            }

            JSONArray devDetailsArray = new JSONArray();
            Connection conn = DriverManager.getConnection(url, user, password);

            if (devSql != null) {
                PreparedStatement devStmt = conn.prepareStatement(devSql);
                devStmt.setString(1, partid);
                ResultSet devResult = devStmt.executeQuery();

                while (devResult.next()) {
                    String deviationId = devResult.getString("deviationid");
                    if (deviationId == null) {
                        throw new Exception("deviationid is null.");
                    }

                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, deviationId);
                    ResultSet result = stmt.executeQuery();
                    ResultSetMetaData metaData = result.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (result.next()) {
                        JSONArray selectables = new JSONArray();
                        JSONArray attributes = new JSONArray();
                        JSONArray relAttributes = new JSONArray();  // Create a new array for each result
                        JSONObject devDetails = new JSONObject();
                        String id = result.getString("deviationid");

                        for (int i = 1; i <= columnCount; i++) {
                            String Name = metaData.getColumnName(i);
                            String Value = result.getString(i);
                            if (Name.equalsIgnoreCase("deviationid")) {
                                continue;
                            }
                            String displayName = displayNamesMap.get(Name);
                            if (displayName == null) {
                                displayName = Name;
                            }

                            JSONObject devattr = new JSONObject();
                            devattr.put("name", Name);
                            devattr.put("value", Value);
                            devattr.put("displayname", displayName);

                            if (isSelectable(Name)) {
                                selectables.put(devattr);
                            } else {
                                attributes.put(devattr);
                            }
                        }

                        // Fetch relattributes
                        PreparedStatement relStmt = conn.prepareStatement(relSql);
                        relStmt.setString(1, deviationId);
                        ResultSet relResult = relStmt.executeQuery();
                        ResultSetMetaData relMetaData = relResult.getMetaData();
                        int relColumnCount = relMetaData.getColumnCount();

                        while (relResult.next()) {
                            for (int j = 1; j <= relColumnCount; j++) {
                                String relName = relMetaData.getColumnName(j);
                                String relValue = relResult.getString(j);
                                if (relName.equalsIgnoreCase("id") || relName.equalsIgnoreCase("deviationid")) {
                                    continue;
                                }
                                String relDisplayName = displayNamesMap.get(relName);
                                if (relDisplayName == null) {
                                    relDisplayName = relName;
                                }

                                JSONObject relAttr = new JSONObject();
                                relAttr.put("name", relName);
                                relAttr.put("value", relValue);
                                relAttr.put("displayname", relDisplayName);
                                relAttributes.put(relAttr);
                            }
                        }

                        devDetails.put("selectables", selectables);
                        devDetails.put("attributes", attributes);
                        devDetails.put("relattributes", relAttributes);
                        devDetails.put("partid", partid);
                       
                        devDetailsArray.put(new JSONObject().put("objectId: " + id, devDetails));
                    }
                    responseObject.put("objectdetails", devDetailsArray);
                }
            } else {
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet result = stmt.executeQuery();
                ResultSetMetaData metaData = result.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (result.next()) {
                    JSONArray selectables = new JSONArray();
                    JSONArray attributes = new JSONArray();
                    JSONArray relAttributes = new JSONArray();  // Create a new array for each result
                    JSONObject devDetails = new JSONObject();
                    String id = result.getString("deviationid");

                    for (int i = 1; i <= columnCount; i++) {
                        String Name = metaData.getColumnName(i);
                        String Value = result.getString(i);
                        if (Name.equalsIgnoreCase("deviationid")) {
                            continue;
                        }
                        String displayName = displayNamesMap.get(Name);
                        if (displayName == null) {
                            displayName = Name;
                        }

                        JSONObject devattr = new JSONObject();
                        devattr.put("name", Name);
                        devattr.put("value", Value);
                        devattr.put("displayname", displayName);

                        if (isSelectable(Name)) {
                            selectables.put(devattr);
                        } else {
                            attributes.put(devattr);
                        }
                    }

                    // Fetch relattributes
                    PreparedStatement relStmt = conn.prepareStatement(relSql);
                    relStmt.setString(1, id);
                    ResultSet relResult = relStmt.executeQuery();
                    ResultSetMetaData relMetaData = relResult.getMetaData();
                    int relColumnCount = relMetaData.getColumnCount();

                    while (relResult.next()) {
                        for (int j = 1; j <= relColumnCount; j++) {
                            String relName = relMetaData.getColumnName(j);
                            String relValue = relResult.getString(j);
                            if (relName.equalsIgnoreCase("id") || relName.equalsIgnoreCase("deviationid")) {
                                continue;
                            }
                            String relDisplayName = displayNamesMap.get(relName);
                            if (relDisplayName == null) {
                                relDisplayName = relName;
                            }

                            JSONObject relAttr = new JSONObject();
                            relAttr.put("name", relName);
                            relAttr.put("value", relValue);
                            relAttr.put("displayname", relDisplayName);
                            relAttributes.put(relAttr);
                        }
                    }

                    devDetails.put("selectables", selectables);
                    devDetails.put("attributes", attributes);
                    devDetails.put("relattributes", relAttributes);
                    devDetails.put("partid", partid);
                    devDetailsArray.put(new JSONObject().put("objectId: " + id, devDetails));
                }
                responseObject.put("objectdetails", devDetailsArray);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error occurred while fetching deviation details.", e);
        }

        return responseObject.toString();
    }
    
    @GET
    @Path("Specfile")
    @Produces(MediaType.APPLICATION_JSON)
    public  void main(String[] args) throws Exception{
    	String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String user= System.getenv("SupplierPortalDBUsername");

    	 Properties properties = new Properties();
    	 InputStream propertiesData = getClass().getClassLoader().getResourceAsStream("Portal.properties");
    	 properties.load(propertiesData);
    	 Class.forName("org.postgresql.Driver");
    	 String filepath = properties.getProperty("Filepath");
         File file = new File(filepath);
         try {
         Connection conn = DriverManager.getConnection(url, user, password);
         FileInputStream fis = new FileInputStream(file);
         PreparedStatement ps = conn.prepareStatement(
             "INSERT INTO files (file_name, file_size, content_type, file_data) VALUES (?, ?, ?, ?)");
         ps.setString(1, file.getName());
         ps.setLong(2, file.length());
         ps.setString(3, getFileContentType(file)); // Use a function to determine the content type
         ps.setBinaryStream(4, fis, (int) file.length());

         ps.executeUpdate();
         System.out.println("File uploaded successfully!");

         } catch (Exception e) {
        	 e.printStackTrace();
         }
    }
    
    private static String getFileContentType(File file) {
        // Determine the content type based on the file extension
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return "application/msword";
        } else if (fileName.endsWith(".zip")) {
            return "application/zip";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream"; // Default binary file type
    }

}// main class Ends