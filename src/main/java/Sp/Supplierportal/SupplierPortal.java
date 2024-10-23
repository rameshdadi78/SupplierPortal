
package Sp.Supplierportal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
	 * 
	 * <p>Example Usage:</p>
	 * 
	 * <pre>
	 * Example URL: http://localhost:8081/Webservice/webapi/parts?column1=value1&column2=value2
	 * 
	 * Response:
	 * {
	 *   "results": [
	 *     {
	 *       "objectId: 1": {
	 *         "basicAttributes": [
	 *           {
	 *             "displayName": "Display Name 1",
	 *             "name": "internalName1",
	 *             "value": "value1"
	 *           },
	 *           {
	 *             "displayName": "Display Name 2",
	 *             "name": "internalName2",
	 *             "value": "value2"
	 *           }
	 *         ],
	 *         "attributes": [
	 *           {
	 *             "displayName": "Display Name 3",
	 *             "name": "internalName3",
	 *             "value": "value3"
	 *           },
	 *           {
	 *             "displayName": "Display Name 4",
	 *             "name": "internalName4",
	 *             "value": "value4"
	 *           }
	 *         ]
	 *       }
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>Note:</p>
	 * <ul>
	 *   <li>The `sp.properties` file should contain mappings for `ecpartcolumns` and `ecpartbasicAttributes`.</li>
	 *   <li>The `Id` column from the database is used as the key for each object in the JSON response.</li>
	 * </ul>
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
	    String mpnTable = pro.getProperty("MpnTable");
	    String connectionTablename = pro.getProperty("MpnconnectionTable");

	    // Load ecpartcolumns and ecpartbasicAttributes
	    String columnsProperty = pro.getProperty("ecpartcolumns");
	    String basicAttributesProperty = pro.getProperty("ecpartbasicAttributes");

	    // Prepare column maps
	    Map<String, Map<String, String>> columnMap = new HashMap<>();
	    Map<String, Map<String, String>> basicAttributeMap = new HashMap<>();
	    
	    for (String mapping : columnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            Map<String, String> detailMap = new HashMap<>();
	            detailMap.put("internalName", parts[1].trim());
	            detailMap.put("displayName", parts[0].trim());
	            columnMap.put(parts[1].trim(), detailMap);
	        }
	    }
	    
	    for (String mapping : basicAttributesProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            Map<String, String> detailMap = new HashMap<>();
	            detailMap.put("internalName", parts[1].trim());
	            detailMap.put("displayName", parts[0].trim());
	            basicAttributeMap.put(parts[1].trim(), detailMap);
	        }
	    }

	    // Extract query parameters
	    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
	    String supplierName = queryParams.getFirst("suppliername"); // Get suppliername from query parameters
	    String partid = queryParams.getFirst("partid"); // Get partid from query parameters

	    // SQL for filtering based on suppliername and partid
	    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tablename).append(" WHERE 1=1");
	    if (partid != null && !partid.trim().isEmpty()) {
	        sql.append(" AND id = '").append(partid).append("'");
	    }

	    JSONArray jsonArray = new JSONArray();
	    ResultSet result = null;
	    Class.forName("org.postgresql.Driver");
	    try (Connection conn = DriverManager.getConnection(url, userName, password);
	         Statement stmt = conn.createStatement()) {

	        result = stmt.executeQuery(sql.toString());

	        while (result.next()) {
	            String id = result.getString("id");
	            // Proceed only if partid is not empty
	            if (id != null && !id.trim().isEmpty()) {
		    boolean isVisible = false;
	                // Fetch additional data based on suppliername
	                if (supplierName != null && !supplierName.trim().isEmpty()) {
	                    String mpnQuery = "SELECT ct.partid " +
	                            "FROM " + connectionTablename + " ct " +
	                            "JOIN " + mpnTable + " mt ON ct.mpnid = mt.mpnid " +
	                            "WHERE ct.partid = ? " +
	                            "AND mt.companyName = ?";
	                    try (PreparedStatement mpnStmt = conn.prepareStatement(mpnQuery)) {
	                        mpnStmt.setString(1, id);
	                        mpnStmt.setString(2, supplierName);
	                        ResultSet mpnResult = mpnStmt.executeQuery();
	                        if (mpnResult.next()) {
	                            isVisible = true;  // Set visibility to true if mpnQuery matches
	                        }
	                    }
	                }

	                // Collect attributes regardless of mpnQuery success
	                JSONObject jsonObject = new JSONObject();
	                JSONArray basicAttributesArray = new JSONArray();
	                JSONArray attributesArray = new JSONArray();
	                String objectId = null;

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
//	                        if (column.equalsIgnoreCase("Id")) {
//	                            continue; // Skip processing the 'Id' column
//	                        }

	                        JSONObject attribute = new JSONObject();
	                        String columnValue = result.getString(column);
	                        Map<String, String> details = columnMap.get(column);
	                        attribute.put("displayName", details.get("displayName"));
	                        attribute.put("name", details.get("internalName"));
	                        attribute.put("value", columnValue);
							if ("Id".equals(details.get("displayName"))) {
		                                	objectId = columnValue; // Insert the objectId
		                    }
	                        attributesArray.put(attribute);
	                    }

	                jsonObject.put("basicAttributes", basicAttributesArray);
	                jsonObject.put("attributes", attributesArray);
	                jsonObject.put("visibilityValue", isVisible);
	                JSONObject idObject = new JSONObject();
	                idObject.put("objectId: " + objectId, jsonObject);
	                
	                jsonArray.put(idObject);
	            }
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





	/**
	 * Retrieves detailed EBOM (Engineering Bill of Materials) records from the database based on the provided query parameters.
	 * The results are returned in a JSON format, including connection attributes, basic attributes, and other attributes.
	 * 
	 * The method dynamically builds a SQL query based on the provided `ebomrelid`, `fromid`, and `connattributes` parameters.
	 * It fetches data from the table specified in the `ebomTable` property of the `sp.properties` file. The attributes for
	 * connection attributes, basic attributes, and other attributes are also defined in the properties file.
	 * 
	 * @param ebomRelId The EBOM relationship ID to filter the records. If not provided or empty, no filter is applied.
	 * @param fromId The ID from which the EBOM is related. If not provided or empty, no filter is applied.
	 * @param connAttributes A boolean flag indicating whether to include connection attributes in the response.
	 * @return A JSON string containing the EBOM details. Each object in the JSON array represents a record from the database
	 *         with attributes based on the provided query parameters.
	 * @throws Exception If there is an error accessing the database, processing the request, or if the properties file is missing.
	 * 
	 * <p>Example Usage:</p>
	 * 
	 * <pre>
	 * Example URL: http://localhost:8081/Webservice/webapi/ebomdetails?ebomrelid=123&fromid=456&connattributes=true
	 * 
	 * Response:
	 * {
	 *   "results": [
	 *     {
	 *       "objectid: 456": {
	 *         "connection attributes": [
	 *           {
	 *             "displayName": "Connection Attribute 1",
	 *             "name": "connAttr1",
	 *             "value": "value1"
	 *           },
	 *           {
	 *             "displayName": "Connection Attribute 2",
	 *             "name": "connAttr2",
	 *             "value": "value2"
	 *           }
	 *         ],
	 *         "basic attributes": [
	 *           {
	 *             "displayName": "Basic Attribute 1",
	 *             "name": "basicAttr1",
	 *             "value": "value3"
	 *           }
	 *         ],
	 *         "attributes": [
	 *           {
	 *             "displayName": "Other Attribute 1",
	 *             "name": "attr1",
	 *             "value": "value4"
	 *           }
	 *         ]
	 *       }
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>Note:</p>
	 * <ul>
	 *   <li>The `sp.properties` file should contain mappings for `ebomTable`, `ebomconnectionAttributes`, `ebombasicAttributes`,
	 *       and `attributes`.</li>
	 *   <li>The `ebomrelid` and `fromid` query parameters are optional and used to filter the records. If they are not provided,
	 *       all records from the table are included in the response.</li>
	 *   <li>If `connAttributes` is set to `true`, connection attributes are included in the response; otherwise, they are omitted.</li>
	 * </ul>
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
	 * Retrieves detailed EBOM (Engineering Bill of Materials) records and associated EC Part details from the database based on
	 * the provided query parameters. The results are returned in JSON format, including connection attributes, basic attributes,
	 * other attributes, and optionally EC Part attributes.
	 * 
	 * This method dynamically constructs a SQL query to fetch records from the EBOM table and, if requested, fetches additional
	 * attributes from the EC Part table. The attributes to be displayed for EC Parts are specified through the `objectAttributes`
	 * parameter and their display names are mapped using properties loaded from the `sp.properties` file.
	 * 
	 * @param ebomRelId The EBOM relationship ID to filter the records. If not provided or empty, no filter is applied.
	 * @param fromId The ID from which the EBOM is related. If not provided or empty, no filter is applied.
	 * @param connAttributes A boolean flag indicating whether to include connection attributes in the response.
	 * @param objectAttributes A comma-separated string specifying the EC Part attributes to be included in the response. 
	 *                         Each attribute is represented by its internal name. If not provided or empty, no EC Part attributes
	 *                         are included.
	 * @return A JSON string containing the EBOM details and optionally EC Part details. Each object in the JSON array represents
	 *         a record from the database with attributes based on the provided query parameters.
	 * @throws Exception If there is an error accessing the database, processing the request, or if the properties file is missing.
	 * 
	 * <p>Example Usage:</p>
	 * 
	 * <pre>
	 * Example URL: http://localhost:8081/Webservice/webapi/ebomretrivalparentcolumns?ebomrelid=123&fromid=456&connattributes=true&objectattributes=attr1,attr2
	 * 
	 * Response:
	 * {
	 *   "results": [
	 *     {
	 *       "objectid: 456": {
	 *         "connection attributes": [
	 *           {
	 *             "displayName": "Connection Attribute 1",
	 *             "name": "connAttr1",
	 *             "value": "value1"
	 *           },
	 *           {
	 *             "displayName": "Connection Attribute 2",
	 *             "name": "connAttr2",
	 *             "value": "value2"
	 *           }
	 *         ],
	 *         "basic attributes": [
	 *           {
	 *             "displayName": "Basic Attribute 1",
	 *             "name": "basicAttr1",
	 *             "value": "value3"
	 *           }
	 *         ],
	 *         "attributes": [
	 *           {
	 *             "displayName": "Other Attribute 1",
	 *             "name": "attr1",
	 *             "value": "value4"
	 *           }
	 *         ],
	 *         "ecpart attributes": [
	 *           {
	 *             "displayName": "EC Part Attribute 1",
	 *             "name": "attr1",
	 *             "value": "ecValue1"
	 *           },
	 *           {
	 *             "displayName": "EC Part Attribute 2",
	 *             "name": "attr2",
	 *             "value": "ecValue2"
	 *           }
	 *         ]
	 *       }
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>Note:</p>
	 * <ul>
	 *   <li>The `sp.properties` file should contain mappings for `ebomTable`, `ebomconnectionAttributes`, `ebombasicAttributes`,
	 *       `attributes`, and `ecpartcolumnsfordisplaythroughebom`.</li>
	 *   <li>The `ebomrelid` and `fromid` query parameters are optional and used to filter the records. If they are not provided,
	 *       all records from the table are included in the response.</li>
	 *   <li>If `connAttributes` is set to `true`, connection attributes are included in the response; otherwise, they are omitted.</li>
	 *   <li>The `objectAttributes` parameter should be a comma-separated list of internal names for EC Part attributes to be included.
	 *       If it is not provided or empty, EC Part attributes are not included.</li>
	 *   <li>The `ecpartcolumnsfordisplaythroughebom` property in the `sp.properties` file maps internal names to display names for
	 *       EC Part attributes.</li>
	 * </ul>
	 */
	
	@GET
	@Path("ebomretrivalparentcolumns")
	@Produces(MediaType.APPLICATION_JSON)
	public String getParentObjectColumnsFromEBOMTable(
	        @QueryParam("fromid") String fromId,
	        @QueryParam("connattributes") boolean connAttributes,
	        @QueryParam("objectattributes") String objectAttributes
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
	    String ecPartTable = properties.getProperty("ecpartTable");
	    
	    // Load EC Part Attributes Display Names from properties
	    String ecPartAttributesDisplayNames = properties.getProperty("ecpartcolumnsfordisplaythroughebom");
	    Map<String, String> ecPartAttributesMap = new HashMap<>();
	    for (String attrPair : ecPartAttributesDisplayNames.split(",")) {
	        String[] attrParts = attrPair.split("\\|");
	        ecPartAttributesMap.put(attrParts[1].trim(), attrParts[0].trim()); // internalName -> displayName
	    }

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ");
	    sql.append(ebomTable).append(" WHERE 1=1");

	  
	    if (fromId != null && !fromId.trim().isEmpty()) {
	        sql.append(" AND fromid = '").append(fromId).append("'");
	    }
	    System.out.println("sql*****************" + sql.toString());

	    ResultSet result = null;
	    ResultSet ecPartResult = null;
	    JSONArray jsonArray = new JSONArray();
	    Connection conn = null;
	    Statement stmt = null;
	    Statement ecPartStmt = null;

	    try {
	        Class.forName("org.postgresql.Driver");
	        conn = DriverManager.getConnection(url, userName, password);
	        stmt = conn.createStatement();
	        
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
	                    String displayName = attrParts[0].trim();
	                    String attr = attrParts[1].trim();
	                    JSONObject attrObject = new JSONObject();
	                    attrObject.put("displayName", displayName);
	                    attrObject.put("name", attr);
	                    attrObject.put("value", result.getString(attr));
	                    connAttrArray.put(attrObject);
	                }
	                objectDetails.put("connection attributes", connAttrArray);
	            }

	            // Process basic attributes
	            for (String attrPair : basicAttributes.split(",")) {
	                String[] attrParts = attrPair.split("\\|");
	                String displayName = attrParts[0].trim();
	                String attr = attrParts[1].trim();
	                JSONObject attrObject = new JSONObject();
	                attrObject.put("displayName", displayName);
	                attrObject.put("name", attr);
	                attrObject.put("value", result.getString(attr));
	                basicAttrArray.put(attrObject);
	            }

	            // Process other attributes
	            for (String attrPair : attributes.split(",")) {
	                String[] attrParts = attrPair.split("\\|");
	                String displayName = attrParts[0].trim();
	                String attr = attrParts[1].trim();
	                if (attr.equalsIgnoreCase("fromid")) {
	                    // Skip processing the 'Id' column
	                    continue;
	                }
	                JSONObject attrObject = new JSONObject();
	                attrObject.put("displayName", displayName);
	                attrObject.put("name", attr);
	                attrObject.put("value", result.getString(attr));
	                attrArray.put(attrObject);
	            }

	            // Process EC_Part_Details attributes if specified
	            if (objectAttributes != null && !objectAttributes.trim().isEmpty()) {
	                String[] objectAttrs = objectAttributes.split(",");
	                String ecPartSql = "SELECT * FROM " + ecPartTable + " WHERE Id = '" + result.getString("fromid") + "'";

	                ecPartStmt = conn.createStatement();
	                ecPartResult = ecPartStmt.executeQuery(ecPartSql);
	                if (ecPartResult.next()) {
	                    JSONArray ecPartAttrArray = new JSONArray();
	                    for (String attr : objectAttrs) {
	                        String internalName = attr.trim();
	                        if (internalName.equalsIgnoreCase("id")) {
	    	                    // Skip processing the 'Id' column
	    	                    continue;
	    	                }
	                        System.out.println("internalName*"+internalName);
	                        String displayName = ecPartAttributesMap.getOrDefault(internalName, internalName); // Use default if not found
	                        System.out.println("displayName*"+displayName);
	                        JSONObject attrObject = new JSONObject();
	                        attrObject.put("displayName", displayName);
	                        attrObject.put("name", internalName);
	                        attrObject.put("value", ecPartResult.getString(internalName));
	                        ecPartAttrArray.put(attrObject);
	                    }
	                    objectDetails.put("ecpart attributes", ecPartAttrArray);
	                }
	            }

	            objectDetails.put("basic attributes", basicAttrArray);
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
	        if (ecPartResult != null) {
	            try {
	                ecPartResult.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        if (stmt != null) {
	            try {
	                stmt.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        if (ecPartStmt != null) {
	            try {
	                ecPartStmt.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        if (conn != null) {
	            try {
	                conn.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    return new JSONObject().put("results", jsonArray).toString();
	}


	/**
	 * RESTful API to fetch person member company details along with associated person details attributes.
	 * This method reads from a properties file to dynamically build and execute an SQL query, 
	 * retrieves data from the database, and returns the results as a JSON response.
	 *
	 * @param uriInfo Contains the query parameters passed to the API endpoint.
	 * @return A JSON string containing the person member company details and associated person details attributes.
	 * @throws Exception If there is an issue loading the properties file or executing the SQL query.
	 *
	 * Example usage:
	 * 
	 * Suppose the following URL is used to make a GET request:
	 * 
	 * http://localhost:8080/api/getpersonmembercompanydetails?PersonId=12345
	 * 
	 * The properties file (`sp.properties`) includes:
	 * 
	 * supplierpersonmembercompanydetailsTable=Person_Member_Company_Details
	 * supplierpersonmembercompanydetailscolumns=Type|type,Name|name,Revision|rev,Policy|policy,State|state,Organization|organization,Project|project,Owner|owner
	 * supplierpersonmembercompanydetailsattributes=Attribute1|attribute1,Attribute2|attribute2
	 * 
	 * The response might look like:
	 * 
	 * {
	 *   "results": [
	 *     {
	 *       "personid: 12345": {
	 *         "attributes": [
	 *           {
	 *             "displayName": "Type",
	 *             "name": "type",
	 *             "value": "Employee"
	 *           },
	 *           {
	 *             "displayName": "Name",
	 *             "name": "name",
	 *             "value": "John Doe"
	 *           }
	 *           // ... other attributes
	 *         ],
	 *         "Person Details attribute": [
	 *           {
	 *             "displayName": "Attribute1",
	 *             "name": "attribute1",
	 *             "value": "Value1"
	 *           },
	 *           {
	 *             "displayName": "Attribute2",
	 *             "name": "attribute2",
	 *             "value": "Value2"
	 *           }
	 *           // ... other person details attributes
	 *         ]
	 *       }
	 *     }
	 *     // ... other person member company details
	 *   ]
	 * }
	 */
	
	@GET
	@Path("getpersonmembercompanydetails")
	@Produces(MediaType.APPLICATION_JSON)
	public String getPersonMemberCompanyDetails(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalSPDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    // Load properties file
	    Properties pro = new Properties();
	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	    if (input == null) {
	        throw new FileNotFoundException("sp.properties file not found.");
	    }
	    pro.load(input);

	    String tableName = pro.getProperty("supplierpersonmembercompanydetailsTable");

	    // Load supplierpersonmembercompanydetails columns 
	    String columnsProperty = pro.getProperty("supplierpersonmembercompanydetailscolumns");

	    // Load supplierpersondetails columns for Person Details attribute
	    String personDetailsColumnsProperty = pro.getProperty("supplierpersonmembercompanydetailsattributes");

	    // Map to store the column names and their display names for supplierpersonmembercompanydetails
	    Map<String, String> columnMap = new HashMap<>();
	    for (String mapping : columnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            columnMap.put(parts[1].trim(), parts[0].trim());
	        }
	    }

	    // Map to store the column names and their display names for Supplier_Person_Details
	    Map<String, String> personDetailsColumnMap = new HashMap<>();
	    for (String mapping : personDetailsColumnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            personDetailsColumnMap.put(parts[1].trim(), parts[0].trim());
	        }
	    }

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE 1=1");
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
	        Class.forName("org.postgresql.Driver");
	        Connection conn = DriverManager.getConnection(url, userName, password);
	        Statement stmt = conn.createStatement();
	        result = stmt.executeQuery(sql.toString());

	        while (result.next()) {
	            String id = result.getString("PersonId"); // Extract the PersonId value

	            JSONObject jsonObject = new JSONObject();
	            JSONArray attributesArray = new JSONArray();
	            JSONArray personDetailsArray = new JSONArray();

	            // Add attributes processing
	            for (String column : columnMap.keySet()) {
	            	  if (column.equalsIgnoreCase("personid")) {
		                    // Skip processing the 'Id' column
		                    continue;
		                }
	                JSONObject attribute = new JSONObject();
	                String columnValue = result.getString(column);
	                String displayName = columnMap.get(column);
	                
	                attribute.put("displayName", displayName); // Add display name
	                attribute.put("name", column);     // Add internal name
	                attribute.put("value", columnValue);       // Add value
	                
	                attributesArray.put(attribute);
	            }

	            // Retrieve all columns from Supplier_Person_Details based on PersonId
	            String personDetailsQuery = "SELECT * FROM Supplier_Person_Details WHERE PersonId = '" + id + "'";
	            ResultSet personDetailsResult = stmt.executeQuery(personDetailsQuery);
	            ResultSetMetaData metaData = personDetailsResult.getMetaData();
	            int columnCount = metaData.getColumnCount();

	            if (personDetailsResult.next()) {
	                for (int i = 1; i <= columnCount; i++) {
	                    String columnName = metaData.getColumnName(i);
	                    String columnValue = personDetailsResult.getString(columnName);
	                    if (columnName.equalsIgnoreCase("personid")) {
		                    // Skip processing the 'Id' column
		                    continue;
		                }
	                    JSONObject personDetail = new JSONObject();
	                    String displayName = personDetailsColumnMap.getOrDefault(columnName, columnName); // Get display name from map or use column name
	                    personDetail.put("displayName", displayName);
	                    personDetail.put("name", columnName.toLowerCase()); // Lowercase name for consistency
	                    personDetail.put("value", columnValue);
	                    personDetailsArray.put(personDetail);
	                }
	            }
	            personDetailsResult.close();

	            jsonObject.put("attributes", attributesArray);
	            jsonObject.put("Person Details attribute", personDetailsArray);

	            JSONObject idObject = new JSONObject();
	            idObject.put("personid: " + id, jsonObject); // Use PersonId as the key
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


	/**
	 * Retrieves details of supplier persons from the `supplierpersondetailsTable` based on the provided query parameters.
	 * The results are returned in JSON format, including both basic attributes and other attributes dynamically mapped from columns specified in the properties file.
	 * 
	 * This method constructs a SQL query dynamically based on the query parameters and column mappings defined in the `sp.properties` file.
	 * It then fetches records from the database and formats them into JSON, including basic attributes and additional attributes with their display names.
	 * 
	 * @param uriInfo The URI information containing the query parameters to filter the database records.
	 * @return A JSON string containing the details of supplier persons. Each entry in the JSON array represents a record,
	 *         with basic attributes and additional attributes formatted based on the column mappings.
	 * @throws Exception If there is an error accessing the database, processing the request, or if the properties file is missing.
	 * 
	 * <p>Example Usage:</p>
	 * 
	 * <pre>
	 * Example URL: http://localhost:8081/Webservice/webapi/getsupplierpersondetails?PersonId=456&CompanyName=GlobalTech
	 * 
	 * Response:
	 * {
	 *   "results": [
	 *     {
	 *       "personId: 456": {
	 *         "basicAttributes": [
	 *           {
	 *             "displayName": "Person ID",
	 *             "internalName": "PersonId",
	 *             "value": "456"
	 *           },
	 *           {
	 *             "displayName": "First Name",
	 *             "internalName": "FirstName",
	 *             "value": "John"
	 *           },
	 *           {
	 *             "displayName": "Last Name",
	 *             "internalName": "LastName",
	 *             "value": "Doe"
	 *           }
	 *         ],
	 *         "attributes": [
	 *           {
	 *             "displayName": "Company Name",
	 *             "name": "CompanyName",
	 *             "value": "GlobalTech"
	 *           },
	 *           {
	 *             "displayName": "Email",
	 *             "name": "Email",
	 *             "value": "john.doe@globaltech.com"
	 *           }
	 *         ]
	 *       }
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>Note:</p>
	 * <ul>
	 *   <li>The `sp.properties` file should contain mappings for `supplierpersondetailsTable`, `supplierpersonAttributedetailscolumns`, and `supplierpersonBasicAttributedetailscolumns`.</li>
	 *   <li>The `supplierpersonAttributedetailscolumns` property should be in the format `displayName|internalName,displayName|internalName,...` for additional attributes.</li>
	 *   <li>The `supplierpersonBasicAttributedetailscolumns` property should be in the format `displayName|internalName,displayName|internalName,...` for basic attributes.</li>
	 *   <li>The query parameters are dynamically appended to the SQL query to filter the records from the database. For example, `PersonId=456` will filter records where the `PersonId` column is equal to `456`.</li>
	 *   <li>The `PersonId` column from the result set is used as the key in the JSON response object.</li>
	 * </ul>
	 */
	
	@GET
	@Path("getsupplierpersondetails")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAllSupplierPersonDetails(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalSPDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    // Load properties file
	    Properties pro = new Properties();
	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	    if (input == null) {
	        throw new FileNotFoundException("sp.properties file not found.");
	    }
	    pro.load(input);

	    String tableName = pro.getProperty("supplierpersondetailsTable");

	    // Load supplierpersondetails columns and basic attributes
	    String columnsProperty = pro.getProperty("supplierpersonAttributedetailscolumns");
	    String basicAttributesProperty = pro.getProperty("supplierpersonBasicAttributedetailscolumns");

	    // Map to store the column names and their display names
	    Map<String, String> columnMap = new HashMap<>();
	    for (String mapping : columnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            columnMap.put(parts[1].trim(), parts[0].trim());
	        }
	    }

	    // Map to store the basic attribute column names and their display names
	    Map<String, String> basicAttributeMap = new HashMap<>();
	    for (String mapping : basicAttributesProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            basicAttributeMap.put(parts[1].trim(), parts[0].trim());
	        }
	    }

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE 1=1");
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
	        Class.forName("org.postgresql.Driver");
	        Connection conn = DriverManager.getConnection(url, userName, password);
	        Statement stmt = conn.createStatement();
	        result = stmt.executeQuery(sql.toString());

	        while (result.next()) {
	            String id = result.getString("PersonId"); // Extract the PersonId value

	            JSONObject jsonObject = new JSONObject();
	            JSONArray basicAttributesArray = new JSONArray();
	            JSONArray attributesArray = new JSONArray();

	            // Add basic attributes
	            for (String column : basicAttributeMap.keySet()) {
	                JSONObject attribute = new JSONObject();
	                String columnValue = result.getString(column);
	                String displayName = basicAttributeMap.get(column);
	                
	                attribute.put("displayName", displayName); // Add display name
	                attribute.put("name", column);     // Add internal name
	                attribute.put("value", columnValue);       // Add value
	                
	                basicAttributesArray.put(attribute);
	            }

	            // Add other attributes
	            for (String column : columnMap.keySet()) {
	            	if (column.equalsIgnoreCase("personid")) {
	                    // Skip processing the 'Id' column
	                    continue;
	                }
	            	JSONObject attribute = new JSONObject();
	                String columnValue = result.getString(column);
	                String displayName = columnMap.get(column);
	                
	                attribute.put("displayName", displayName); // Add display name
	                attribute.put("name", column);     // Add internal name
	                attribute.put("value", columnValue);       // Add value
	                
	                attributesArray.put(attribute);
	            }

	            jsonObject.put("basicattributes", basicAttributesArray);
	            jsonObject.put("attributes", attributesArray);
	            
	            JSONObject idObject = new JSONObject();
	            idObject.put("personid: " + id, jsonObject); // Use PersonId as the key
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



	@POST
	@Path("search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getData(String s) 
	        throws IOException, ClassNotFoundException {

	    // Validate input JSON
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
	    String text = json.optString("text", "%").replace("*", "%"); // Replace '*' with '%' for SQL LIKE
	    String field = json.optString("field");
	    String suppliername = json.optString("suppliername");

	    // Input validation for text and suppliername
	    if (text.length() < 3 || suppliername == null || suppliername.trim().isEmpty()) {
	        JSONObject errorResponse = new JSONObject();
	        errorResponse.put("status", "fail");
	        errorResponse.put("message", "Provide at least 3 characters and a suppliername.");
	        return Response.status(Response.Status.BAD_REQUEST)
	                .entity(errorResponse.toString())
	                .type(MediaType.APPLICATION_JSON)
	                .build();
	    }

	    // Load properties
	    Properties pro = new Properties();
	    try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
	        if (input == null) throw new FileNotFoundException("sp.properties file not found.");
	        pro.load(input);
	    }

	    String ecPartTable = pro.getProperty("ecpartTable");
	    String mpnTable = pro.getProperty("MpnTable");
	    String connectionTable = pro.getProperty("MpnconnectionTable");
	    String deviationTable = pro.getProperty("Ca_supplier_Table");
	    String supplierConnectionTable = pro.getProperty("Ca_supplier_Table");

	    JSONArray jsonArray = new JSONArray();
		String url=System.getenv("SupplierPortalDBURL");
		String postgresPass=System.getenv("SupplierPortalDBPassword");
		String postgresUser= System.getenv("SupplierPortalDBUsername");
	    Class.forName("org.postgresql.Driver");

	    try (Connection con = DriverManager.getConnection(url, postgresUser, postgresPass)) {
	        String sql = "";
	        PreparedStatement ps = null;

	        // Build SQL based on field
	        if (field.equalsIgnoreCase("Everything")) {
	            StringBuilder sqlBuilder = new StringBuilder("SELECT id, name, type FROM " + ecPartTable + " WHERE ");
	            List<String> conditions = new ArrayList<>();

	            // Generate LIKE condition for all columns
	            try (PreparedStatement columnPs = con.prepareStatement(
	                    "SELECT column_name FROM information_schema.columns WHERE table_name = '" + ecPartTable + "'")) {
	                ResultSet rs = columnPs.executeQuery();
	                while (rs.next()) {
	                    String columnName = rs.getString("column_name");
	                    conditions.add(columnName + " ILIKE ?");
	                }
	            }

	            sqlBuilder.append(String.join(" OR ", conditions));
	            sql = sqlBuilder.toString();
	            ps = con.prepareStatement(sql);
	            for (int i = 0; i < conditions.size(); i++) {
	                ps.setString(i + 1, "%" + text + "%");
	            }

	        } else if (field.equalsIgnoreCase("name")) {
	            sql = "SELECT id, name, type FROM " + ecPartTable + " WHERE name ILIKE ?";
	            ps = con.prepareStatement(sql);
	            ps.setString(1, "%" + text + "%");

	        } else if (field.equalsIgnoreCase("Ids")) {
	            sql = "SELECT id, type FROM " + ecPartTable + " WHERE id ILIKE ?";
	            ps = con.prepareStatement(sql);
	            ps.setString(1, "%" + text + "%");
	        }

	        if (ps != null) {
	            ResultSet set = ps.executeQuery();
	            Map<String, List<String>> partsByType = new HashMap<>();

	            while (set.next()) {
	                String partid = set.getString("id");
	                String type = set.getString("type");

	                // Check visibility for each partid
	                boolean isVisible = false, specVisible = false, itemVisible = false;
					String matchingItemPartId = null;
					String matchingSpecPartId = null;
	                // Step 1: Check in MPN and connection tables
	                String visibilitySql = "SELECT ct.partid FROM " + connectionTable + " ct " +
	                        "JOIN " + mpnTable + " mt ON ct.mpnid = mt.mpnid " +
	                        "WHERE ct.partid = ? AND mt.CompanyName = ?";

	                try (PreparedStatement stmt = con.prepareStatement(visibilitySql)) {
	                    stmt.setString(1, partid);
	                    stmt.setString(2, suppliername);

	                    try (ResultSet rs = stmt.executeQuery()) {
	                        if (rs.next()) isVisible = true; // Part found in tables
	                    }
	                }

	                // Step 2: Check in deviation table for spec/item visibility
	                if (!isVisible) {
	                    String supplierSql = "SELECT acknowledge, supplier_item_visibility, supplier_spec_visibility " +
	                            "FROM " + supplierConnectionTable + " WHERE supplier_item_visibility LIKE ? " +
	                            "OR supplier_spec_visibility LIKE ?";

	                    String pattern = "%" + partid + "%";

	                    try (PreparedStatement supStmt = con.prepareStatement(supplierSql)) {
	                        supStmt.setString(1, pattern);
	                        supStmt.setString(2, pattern);

	                        try (ResultSet suppRs = supStmt.executeQuery()) {
	                            while (suppRs.next()) {
	                                String acknowledge = suppRs.getString("acknowledge");
	                                String spec = suppRs.getString("supplier_spec_visibility");
	                                String item = suppRs.getString("supplier_item_visibility");

	                                specVisible = spec != null && spec.contains(partid);
	                                itemVisible = item != null && item.contains(partid);
									
	                                if ("yes".equalsIgnoreCase(acknowledge)) {
	                                    isVisible = true;
	                                    break;
	                                }
	                            }
	                        }
	                    }
	                }

	                // Store parts by type and prepare JSON object
	                partsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(partid);

	                JSONObject partObject = new JSONObject();
	                partObject.put("partid", partid);
	                partObject.put("isVisible", isVisible);
	                partObject.put("specVisible", specVisible);
	                partObject.put("itemVisible", itemVisible);

	                JSONObject jsonObject = new JSONObject();
	                jsonObject.put("type: " + type, partObject);
	                jsonArray.put(jsonObject);
	            }
	        }

	        // Construct final JSON response
	        JSONObject responseJson = new JSONObject();
	        responseJson.put("results", jsonArray);
	        return Response.ok(responseJson.toString(), MediaType.APPLICATION_JSON).build();

	    } catch (Exception e) {
	        e.printStackTrace();
	        JSONObject errorResponse = new JSONObject();
	        errorResponse.put("status", "fail");
	        errorResponse.put("message", "An error occurred while processing your request.");
	        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	                .entity(errorResponse.toString())
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
        // Retrieve database credentials from environment variables
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
	    String detailsColumnsProperty = pro.getProperty("cadetails");
	    String supplierPerson = pro.getProperty("supplierpersondetailsTable");
	    String caSupplier = pro.getProperty("casuppliersdetailsTable");
	    String changeActionTable = pro.getProperty("catable");
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


	    // Get email address from query parameter
	    String email = uriInfo.getQueryParameters().getFirst("email");
	    if (email == null || email.trim().isEmpty()) {
	        return "{ \"error\": \"Missing or empty email parameter\" }";
	    }

	    // Query combining both person details and supplier details using JOIN
	    String supplierDetailsQuery = "SELECT casd.acknowledge, ca.* " +
                "FROM " + supplierPerson + " spd " +
                "JOIN " + caSupplier + " casd ON spd.companyid = casd.companyid " +
                "JOIN " + changeActionTable + " ca ON ca.changeNumber = casd.changeNumber " +
                "WHERE spd.email_address = ?";


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
	            jsonObject.put("attributes", attributesArray);
	            jsonObject.put("connectionattributes", connectionAttributesArray);
	        
	            JSONObject idObject = new JSONObject();
	            idObject.put("caid: " + supplierResult.getString("changeNumber"),jsonObject);  // Use companyid as the key
	            jsonArray.put(idObject);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
	    }

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
        // Retrieve database credentials from environment variables
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
	    
	    String caTable = pro.getProperty("catable");

	    // Load column mappings for ca_suppliers_details attributes
	    String detailsColumnsProperty = pro.getProperty("ca_attribute");
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

	    // Query to fetch the required data
	    String caDetailsQuery = "SELECT * FROM "+ caTable +" ca WHERE ca.changeNumber = ?";

	    JSONArray jsonArray = new JSONArray();
	    Class.forName("org.postgresql.Driver");

	    try (Connection conn = DriverManager.getConnection(url, userName, password);
	         PreparedStatement Stmt = conn.prepareStatement(caDetailsQuery)) {

	        // Set the caID parameter
	    	Stmt.setString(1, caID);
	        ResultSet Result = Stmt.executeQuery();

	        // Process the result set
	        while (Result.next()) {
	            JSONObject jsonObject = new JSONObject();
	            JSONArray attributesArray = new JSONArray();

	            // Add attributes from ca_suppliers_details
	            for (String column : detailsColumnMap.keySet()) {
	                String columnValue = Result.getString(column);

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
	            JSONObject idObject = new JSONObject();
	            idObject.put("id: " + Result.getString("changeNumber"), jsonObject);
	            jsonArray.put(idObject);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
	    }

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

	        
	        // Load properties file
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

	        String tablename = pro.getProperty("casuppliersdetailsTable");

	        // Parse the JSON input
	        JSONObject jsonObject = new JSONObject(jsonInput);
	        String acknowledgedBy = jsonObject.getString("username");
	        
	        // Remove everything after the '@' in the email
	        String username = acknowledgedBy.split("@")[0];

	        String objectId = jsonObject.getString("objectId");
	        String value = jsonObject.getString("value");
	        if ("true".equals(value)) {
	            value = "Yes";
	        } else if ("false".equals(value)) {
	            value = "No";
	        }
	        

	        // Connect to PostgreSQL database and update the record
	        String updateSQL = "UPDATE " + tablename + 
	                           " SET acknowledge = ?, acknowledgedby = ? " +
	                           "WHERE changenumber = ?";
	        Class.forName("org.postgresql.Driver");
	        try (Connection conn = DriverManager.getConnection(url, userName, password);
	             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
	             
	            pstmt.setString(1, value); // Acknowledge value
	            pstmt.setString(2, username); // Use the extracted username
	            pstmt.setString(3, objectId); // Change number (objectId)

	            int rowsAffected = pstmt.executeUpdate();
	            if (rowsAffected > 0) {
	                return Response.ok("Update successful").build();
	            } else {
	                return Response.status(Response.Status.NOT_FOUND)
	                               .entity("No records updated").build();
	            }
	        } catch (SQLException e) {
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
		String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);
		    String tablename = pro.getProperty("PartTable");
		    String supplierTable = pro.getProperty("Ca_supplier_Table");

		    // Load ecpartcolumns and ecpartbasicAttributes
		    String columnsProperty = pro.getProperty("partcolumns");
		    String basicAttributesProperty = pro.getProperty("partbasicAttributes");

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
		    String caId = uriInfo.getQueryParameters().getFirst("caid");
		    if (caId == null || caId.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty email parameter\" }";
		    }
		    // Build the SQL query dynamically based on provided query parameters
//		    String caDetailsQuery =  "SELECT epd.*, csd.supplier_visibility, csd.supplier_item_visibility, csd.supplier_spec_visibility " +
//		            "FROM " + tablename + " epd " +
//		            "JOIN " + supplierTable + " csd " +
//		            "ON epd.name = csd.name " +
//		            "WHERE (? = epd.changenumber OR " +
//            "     epd.changenumber LIKE CONCAT('%|', ?, '|%') OR " + // in the middle
//            "     epd.changenumber LIKE CONCAT(?, '|%') OR " +      // at the beginning
//            "     epd.changenumber LIKE CONCAT('%|', ?))";          // at the end

		    String caDetailsQuery =  "SELECT epd.*, csd.supplier_visibility, csd.supplier_item_visibility, csd.supplier_spec_visibility " +
                    "FROM " + tablename + " epd " +
                    "JOIN " + supplierTable + " csd " +
                    "ON (epd.changenumber = csd.changenumber OR " +
                    "     epd.changenumber LIKE CONCAT('%|', csd.changenumber, '|%') OR " + // in the middle
                    "     epd.changenumber LIKE CONCAT(csd.changenumber, '|%') OR " +      // at the beginning
                    "     epd.changenumber LIKE CONCAT('%|', csd.changenumber)) " +        // at the end
                    "WHERE (? = epd.changenumber OR " +
                    "     epd.changenumber LIKE CONCAT('%|', ?, '|%') OR " + // in the middle
                    "     epd.changenumber LIKE CONCAT(?, '|%') OR " +      // at the beginning
                    "     epd.changenumber LIKE CONCAT('%|', ?))";          // at the end



		    JSONArray jsonArray = new JSONArray();
		    Class.forName("org.postgresql.Driver");

		    try (Connection conn = DriverManager.getConnection(url, userName, password);
		         PreparedStatement Stmt = conn.prepareStatement(caDetailsQuery)) {

		        // Set the caID parameter
		    	Stmt.setString(1, caId);
		    	Stmt.setString(2, caId);
		    	Stmt.setString(3, caId);
		    	Stmt.setString(4, caId);
		    	
		        ResultSet Result = Stmt.executeQuery();


		        while (Result.next()) {
		            String id = Result.getString("id"); // Extract the Id value
		            
		            JSONObject jsonObject = new JSONObject();
		            JSONArray basicAttributesArray = new JSONArray();
		            JSONArray attributesArray = new JSONArray();

		            // Add basic attributes
		            for (String column : basicAttributeMap.keySet()) {
		                JSONObject attribute = new JSONObject();
		                String columnValue = Result.getString(column);
		                Map<String, String> details = basicAttributeMap.get(column);
		                attribute.put("displayName", details.get("displayName"));
		                attribute.put("name", details.get("internalName"));
		                attribute.put("value", columnValue);
		                basicAttributesArray.put(attribute);
		            }

		            // Add other attributes
		            for (String column : columnMap.keySet()) {
//		                if (column.equalsIgnoreCase("partid")) {
//		                    // Skip processing the 'Id' column
//		                    continue;
//		                }

		                JSONObject attribute = new JSONObject();
		                String columnValue = Result.getString(column);
		                Map<String, String> details = columnMap.get(column);
		                attribute.put("displayName", details.get("displayName"));
		                attribute.put("name", details.get("internalName"));
		                attribute.put("value", columnValue);
		                attributesArray.put(attribute);
		            }


		            jsonObject.put("basicAttributes", basicAttributesArray);
		            jsonObject.put("attributes", attributesArray);
		            
		            JSONObject idObject = new JSONObject();
		            idObject.put("objectId: " + id, jsonObject); // Use Id as the key
		            jsonArray.put(idObject);
		        }
		    } catch (Exception e) {
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


	 	    Properties pro = new Properties();
	 	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	 	    if (input == null) {
	 	        throw new FileNotFoundException("sp.properties file not found.");
	 	    }
	 	    pro.load(input);

	 	    String mpnTable = pro.getProperty("MpnTable");
	 	    String connectionTablename = pro.getProperty("MpnconnectionTable");
	 	    String caSuppliersTable = pro.getProperty("Ca_supplier_Table");

	 	    Class.forName("org.postgresql.Driver");


	 	    JSONObject inputObject = new JSONObject(inputJson);
	 	    String objectIds = inputObject.getString("objectIds");
	 	    String caid = inputObject.getString("caid");

 	    String[] objectIdsArray;
	 	    if (objectIds.contains("|")) {
	 	        objectIdsArray = objectIds.split("\\|");
	 	    } else {
	 	        objectIdsArray = new String[]{objectIds};  // Single objectId
	 	    }

	 	    // Prepare queries
	 	    String Sql = "SELECT mpn.CompanyName " +
	 	               "FROM " + connectionTablename + " AS related_parts " +
	 	               "JOIN " + mpnTable + " AS mpn " +
	 	               "ON related_parts.MPNID = mpn.MPNID " +
	 	               "WHERE related_parts.Partid = ?";
	 	    
	 	    String sql2 = "SELECT supplier_visibility, supplier_item_visibility, supplier_spec_visibility " +
	 	               "FROM " + caSuppliersTable + " WHERE changenumber= ? AND company_name = ?";
	 	    
	 	    String sql3 = "SELECT supplier_visibility, supplier_item_visibility, supplier_spec_visibility " +
	 	               "FROM " + caSuppliersTable +
	 	               " WHERE (supplier_item_visibility LIKE ? OR supplier_spec_visibility LIKE ?)";

	 	    // JSON response structure
	 	    JSONObject jsonResponse = new JSONObject();
	 	    JSONArray resultsArray = new JSONArray();

	 	    try (Connection conn = DriverManager.getConnection(url, userName, password);
	 	         PreparedStatement joinStmt = conn.prepareStatement(Sql);
	 	         PreparedStatement ps2 = conn.prepareStatement(sql2);
	 	         PreparedStatement ps3 = conn.prepareStatement(sql3)) {

	 	        // Loop through each object ID
	 	        for (String partid : objectIdsArray) {
	 	            joinStmt.setString(1, partid);
	 	            ResultSet joinResultSet = joinStmt.executeQuery();

	 	            JSONObject resultObject = new JSONObject();
	 	            JSONObject idObject = new JSONObject();  // This will hold the attributes under the part ID
	 	            JSONObject attributes = new JSONObject(); // Initialize attributes with default values
	 	            attributes.put("supplier", false);
	 	            attributes.put("supplieritem", false);
	 	            attributes.put("supplierspec", false);

	 	            if (joinResultSet.next()) {
	 	                String manufacturerName = joinResultSet.getString("CompanyName");

	 	                
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
	 	                    
	 	                    String partidLike = "%" + partid + "%";
	 	                    ps3.setString(1, partidLike); // Check if partid exists in supplier_item_visibility
	 	                    ps3.setString(2, partidLike); // Check if partid exists in supplier_spec_visibility
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
	 	        e.printStackTrace();
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
		    String detailsColumnsProperty = pro.getProperty("Deviation_Details");
		    String supplierTable = pro.getProperty("supplierpersondetailsTable");
		    String caSupplierTable = pro.getProperty("casuppliersdetailsTable");
		    String devaitionTable = pro.getProperty("deviationTable");
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

		    // Get email address from query parameter
		    String email = uriInfo.getQueryParameters().getFirst("email");
		    if (email == null || email.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty email parameter\" }";
		    }

		    // Query combining both person details and supplier details using JOIN
		    String supplierDetailsQuery = "SELECT casd.acknowledge, dev.* " +
	                "FROM " + supplierTable + " spd " +
	                "JOIN " + caSupplierTable + " casd ON spd.companyid = casd.companyid " +
	                "JOIN "+ devaitionTable + "  dev ON dev.deviationid = casd.changenumber " +
	                "WHERE spd.email_address = ?";


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
		            jsonObject.put("attributes", attributesArray);
		        
		            JSONObject idObject = new JSONObject();
		            idObject.put("deviationid: " + supplierResult.getString("deviationid"),jsonObject);  // Use companyid as the key
		            jsonArray.put(idObject);
		        }
		    } catch (SQLException e) {
		        e.printStackTrace();
		        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
		    }

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
		    
		    String devTable = pro.getProperty("deviationTable");

		    // Load column mappings for ca_suppliers_details attributes
		    String detailsColumnsProperty = pro.getProperty("Attributes_Deviation");
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
		    String devID = uriInfo.getQueryParameters().getFirst("devid");
		    if (devID == null || devID.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty caID parameter\" }";
		    }

		    // Query to fetch the required data
		    String caDetailsQuery = "SELECT * FROM "+ devTable +" dev WHERE dev.deviationid = ?";

		    JSONArray jsonArray = new JSONArray();
		    Class.forName("org.postgresql.Driver");

		    try (Connection conn = DriverManager.getConnection(url, userName, password);
		         PreparedStatement Stmt = conn.prepareStatement(caDetailsQuery)) {

		        // Set the caID parameter
		    	Stmt.setString(1, devID);
		        ResultSet Result = Stmt.executeQuery();

		        // Process the result set
		        while (Result.next()) {
		            JSONObject jsonObject = new JSONObject();
		            JSONArray attributesArray = new JSONArray();

		            // Add attributes from ca_suppliers_details
		            for (String column : detailsColumnMap.keySet()) {
		                String columnValue = Result.getString(column);

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
		            JSONObject idObject = new JSONObject();
		            idObject.put("id: " + Result.getString("deviationid"), jsonObject);
		            jsonArray.put(idObject);
		        }
		    } catch (SQLException e) {
		        e.printStackTrace();
		        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
		    }

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
		    String deviationQuery = "SELECT COUNT(*) " +
		            "FROM " + supplierTable + " spd " +
		            "JOIN " + caSupplierTable + " casd ON spd.companyid = casd.companyid " +
		            "JOIN " + deviationTable + " dev ON dev.deviationid = casd.changenumber " +
		            "WHERE spd.email_address = ?";
		    
		    String changeactionQuery = "SELECT COUNT(*) " +
		            "FROM " + supplierTable + " spd " +
		            "JOIN " + caSupplierTable + " casd ON spd.companyid = casd.companyid " +
		            "JOIN " + changeactionTable + " ca ON ca.ChangeNumber = casd.changenumber " +
		            "WHERE spd.email_address = ?";

		    String ecPartsCountQuery = "SELECT COUNT(*) " +
		            "FROM " + supplierTable + " spd " +
		            "JOIN " + companyDetailsTable + " cd ON spd.companyid = cd.companyid " +
		            "JOIN " + mpnTable + " mpn ON cd.name = mpn.companyName " +
		            "JOIN " + mpnRelatedPartsTable + " mrp ON mpn.mpnid = mrp.mpnid " +
					 "JOIN " + ecPartTable + " part ON part.id=mrp.partid " +
		            "WHERE spd.email_address = ?";

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
		    	 return "{ \"error\": \"Missing or empty caID parameter\" }";		   
		    	}

		    // Establish database connection
		    

		    // SQL query
		    String query = "SELECT c.name " +
		                   "FROM " + supplierPersonTable + " sp " +
		                   "JOIN " + companyTable + " c ON sp.companyid = c.companyid " +
		                   "WHERE sp.email_address = ?";
		    Class.forName("org.postgresql.Driver");
		    Connection conn = DriverManager.getConnection(url, userName, password);
		    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
		        pstmt.setString(1, email);  // Set the email in the query
		        ResultSet resultSet = pstmt.executeQuery();

		        // Create JSON structure
		        JSONObject jsonResponse = new JSONObject();
		        JSONArray resultsArray = new JSONArray();

		        if (resultSet.next()) {
		            String companyName = resultSet.getString("name");
		           
		            // Create JSON object for the result
		            JSONObject resultObject = new JSONObject();
		            resultObject.put("suppliername", companyName);

		            // Add result object to results array
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
		@Path("getAssignedParts")
		@Produces(MediaType.APPLICATION_JSON)
		public String getAssignedParts(@Context UriInfo uriInfo) throws Exception {
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
			String connectionTablename = pro.getProperty("MpnconnectionTable");

		    // Get email from query parameters
		    String suppliername = uriInfo.getQueryParameters().getFirst("suppliername");
		    String partid = uriInfo.getQueryParameters().getFirst("partid");
		    if (suppliername == null || suppliername.trim().isEmpty() || partid == null || partid.trim().isEmpty()) {
		    	 return "{ \"error\": \"Missing or empty caID parameter\" }";
		    }

		    // Establish database connection
		    
	        String sql = "SELECT ct.partid " +
	                     "FROM " + connectionTablename + " ct " +
	                     "JOIN " + mpnTable + " mt ON ct.mpnid = mt.mpnid " +
	                     "WHERE ct.partid = ? " +
	                     "AND mt.manufacturername = ?";

	        String partId = null;
		    Class.forName("org.postgresql.Driver");
		    Connection conn = DriverManager.getConnection(url, userName, password);
		    try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
	            preparedStatement.setString(1, partid);  // Set the partid parameter
	            preparedStatement.setString(2, suppliername);  // Set the suppliername parameter
	            
	            try (ResultSet resultSet = preparedStatement.executeQuery()) {
	                if (resultSet.next()) {
	                
	                    partId = resultSet.getString("partid");  // Retrieve the partid from the result set
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace();  // Handle exceptions appropriately
	        }

	        // Create the desired JSON structure
	        JSONObject jsonResponse = new JSONObject();
	        JSONArray resultsArray = new JSONArray();
	        JSONObject resultObject = new JSONObject();

	        if (partId != null) {
	            resultObject.put("Visibility", "true");
	            resultObject.put("id",partId);
	        } else {
	            resultObject.put("Visibility", "false");
	            resultObject.put("id",partId);
	        }

	        resultsArray.put(resultObject);
	        jsonResponse.put("results", resultsArray);

	        return jsonResponse.toString();
		}
		
		
		@GET
		@Path("getAssignedPartsVisibility")
		@Produces(MediaType.APPLICATION_JSON)
		public String getAssignedPartsVisibility(@Context UriInfo uriInfo) throws Exception {
		    String url = "jdbc:postgresql://localhost:5432/supplierportal";
		    String userName = "postgres";
		    String password = "12345";

		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);

		    // Get table names from properties
		    String mpnTable = pro.getProperty("MpnTable");
		    String connectionTablename = pro.getProperty("MpnconnectionTable");
		    String deviationDetailsTable = pro.getProperty("Ca_supplier_Table");

		    // Get query parameters
		    String suppliername = uriInfo.getQueryParameters().getFirst("suppliername");
		    String partid = uriInfo.getQueryParameters().getFirst("partid");

		    if (suppliername == null || suppliername.trim().isEmpty() || 
		        partid == null || partid.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty suppliername/partid parameter\" }";
		    }

		    Class.forName("org.postgresql.Driver");
		    try (Connection conn = DriverManager.getConnection(url, userName, password)) {

		        // Step 1: Check connection and MPN tables
		        String sql = "SELECT ct.partid " +
		                     "FROM " + connectionTablename + " ct " +
		                     "JOIN " + mpnTable + " mt ON ct.mpnid = mt.mpnid " +
		                     "WHERE ct.partid = ? AND mt.manufacturername = ?";

		        String partId = null;
		        boolean isVisible = false;
		        boolean specVisible = false;
		        boolean itemVisible = false;

		        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
		            stmt.setString(1, partid);
		            stmt.setString(2, suppliername);

		            try (ResultSet rs = stmt.executeQuery()) {
		                if (rs.next()) {
		                    partId = rs.getString("partid");
		                    isVisible = true; // Part found
		                }
		            }
		        }

		        // Step 2: Check in CA_Deviation_Suppliers_Details table
		        if (!isVisible) {
		            String visibilitySQL =
		                "SELECT acknowledge, supplier_item_visibility, supplier_spec_visibility " +
		                "FROM " + deviationDetailsTable + " " +
		                "WHERE supplier_item_visibility LIKE ? OR supplier_spec_visibility LIKE ?";

		            String pattern = "%" + partid + "%";

		            try (PreparedStatement visibilityStmt = conn.prepareStatement(visibilitySQL)) {
		                visibilityStmt.setString(1, pattern);
		                visibilityStmt.setString(2, pattern);

		                try (ResultSet visibilityRS = visibilityStmt.executeQuery()) {
		                    while (visibilityRS.next()) {
		                        String acknowledge = visibilityRS.getString("acknowledge");
		                        String spec = visibilityRS.getString("supplier_spec_visibility");
		                        String item = visibilityRS.getString("supplier_item_visibility");
		                        // Check if the partid exists in spec or item visibility fields
		                        specVisible = spec != null && spec.contains(partid);
		                        itemVisible = item != null && item.contains(partid);

		                        if ("yes".equalsIgnoreCase(acknowledge)) {
		                            isVisible = true;
		                            break;
		                        }
		                    }
		                }
		            }
		        }

		        // Step 3: Create the JSON response
		        JSONObject jsonResponse = new JSONObject();
		        JSONArray resultsArray = new JSONArray();
		        JSONObject resultObject = new JSONObject();

		        resultObject.put("Visibility", isVisible ? "true" : "false");
		        resultObject.put("id", partId != null ? partId : partid);
		        resultObject.put("specVisibility", specVisible ? "true" : "false");
		        resultObject.put("itemVisibility", itemVisible ? "true" : "false");

		        resultsArray.put(resultObject);
		        jsonResponse.put("results", resultsArray);
		        System.out.println(jsonResponse.toString());
		        return jsonResponse.toString();

		    } catch (Exception e) {
		        e.printStackTrace();
		        return "{ \"error\": \"An error occurred while processing the request.\" }";
		    }
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
			Properties pro = new Properties();
			InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
			if (input == null) {
				throw new FileNotFoundException("sp.properties file not found.");
			}
			pro.load(input);

			String changeActionTable = pro.getProperty("catable");
			String supplierTable = pro.getProperty("Ca_supplier_Table");
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
		    String text = json.optString("text", "%");
		    String field = json.optString("field");

		    // Validate 'Name' field input
		    if (field.equalsIgnoreCase("Name")) {
		        if (!text.startsWith("CA-") || text.contains("*00*") ) {
		            JSONObject errorResponse = new JSONObject();
		            errorResponse.put("status", "fail");
		            errorResponse.put("message", "For 'Name' field, the text must start with 'CA-' and cannot contain '*00*'.");
		            return Response.status(Response.Status.BAD_REQUEST)
		                    .entity(errorResponse.toString())
		                    .type(MediaType.APPLICATION_JSON)
		                    .build();
		        }
		    }

		    // Ensure input text is at least 3 characters long
		    if (text.equals("*") || text.length() < 3) {
		        JSONObject errorResponse = new JSONObject();
		        errorResponse.put("status", "fail");
		        errorResponse.put("message", "Please provide at least 3 characters or digits for the search.");
		        return Response.status(Response.Status.BAD_REQUEST)
		                .entity(errorResponse.toString())
		                .type(MediaType.APPLICATION_JSON)
		                .build();
		    }

		    // Prepare wildcard text
		    text = text.replace("*", "%");

        String url = System.getenv("SupplierPortalDBURL");
        String postgresPass = System.getenv("SupplierPortalDBPassword");
        String postgresUser = System.getenv("SupplierPortalDBUsername");


		    JSONArray jsonArray = new JSONArray();
		    Map<String, List<String>> caidsByType = new HashMap<>();

		    try {
		        Class.forName("org.postgresql.Driver");
		        Connection con = DriverManager.getConnection(url, postgresUser, postgresPass);

		        String sql = "";
		        if (field.equalsIgnoreCase("ids")) {
		            sql = "SELECT ChangeNumber, type FROM " + changeActionTable + " WHERE ChangeNumber ILIKE ?";
		        } else if (field.equalsIgnoreCase("name")) {
		            sql = "SELECT ChangeNumber, name, type FROM " + changeActionTable + " WHERE name ILIKE ? AND name ILIKE 'CA-%'";
		        } else if (field.equalsIgnoreCase("Everything")) {
		            sql = "SELECT column_name FROM information_schema.columns WHERE table_name = '" + changeActionTable + "'";
		        }

		        try (PreparedStatement ps = con.prepareStatement(sql)) {
		            if (field.equalsIgnoreCase("ids") || field.equalsIgnoreCase("name")) {
		                ps.setString(1, text);
		            }

		            ResultSet rs = ps.executeQuery();

		            if (field.equalsIgnoreCase("Everything")) {
		                while (rs.next()) {
		                    String columnName = rs.getString("column_name");
		                    String query = "SELECT caid, type FROM changeaction WHERE " + columnName + " ILIKE ?";

		                    try (PreparedStatement ps2 = con.prepareStatement(query)) {
		                        ps2.setString(1, text);
		                        ResultSet innerRs = ps2.executeQuery();

		                        while (innerRs.next()) {
		                            String caid = innerRs.getString("ChangeNumber");
		                            String type = innerRs.getString("type");

		                            // Check if the caid exists in ca_suppliers_details
		                            String checkSql = "SELECT COUNT(*) FROM supplierportal_schema1.CA_Deviation_Suppliers_Details  WHERE changenumber = ? AND (acknowledge = 'Yes' OR acknowledge = 'No')";
		                            try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
		                                checkPs.setString(1, caid);
		                                ResultSet checkSet = checkPs.executeQuery();
		                                if (checkSet.next() && checkSet.getInt(1) > 0) {
		                                    caidsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(caid);
		                                }
		                            }
		                        }
		                    }
		                }
		            } else {
		                while (rs.next()) {
		                    String caid = rs.getString("changenumber");
		                    String type = rs.getString("type");

		                    // Check if the caid exists in ca_suppliers_details
		                    String checkSql = "SELECT COUNT(*) FROM supplierportal_schema1.CA_Deviation_Suppliers_Detail  WHERE changenumber = ? AND (acknowledge = 'Yes' OR acknowledge = 'No')";
		                    try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
		                        checkPs.setString(1, caid);
		                        ResultSet checkSet = checkPs.executeQuery();
		                        if (checkSet.next() && checkSet.getInt(1) > 0) {
		                            caidsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(caid);
		                        }
		                    }
		                }
		            }
		        }

		        // Construct JSON output
		        JSONObject js = new JSONObject();
		        for (Map.Entry<String, List<String>> entry : caidsByType.entrySet()) {
		            String type = entry.getKey();
		            List<String> caids = entry.getValue();

		            JSONObject jsonObject = new JSONObject();
		            JSONObject typeObject = new JSONObject();

		            // Join caids with "|" and add to the JSON object
		            typeObject.put("caid", String.join("|", caids));
		            jsonObject.put("type: " + type, typeObject);

		            jsonArray.put(jsonObject);
		        }

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
			    JSONObject finalResponse = new JSONObject();
			    JSONArray jsonArray = new JSONArray();
			    boolean hasError = false;
				Properties pro = new Properties();
				InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
				if (input == null) {
					throw new FileNotFoundException("sp.properties file not found.");
				}
				pro.load(input);

				String devtablename = pro.getProperty("deviationTable");
				String suppliersDetailsTable = pro.getProperty("casuppliersdetailsTable");

			    // Input validation
			    if (s == null || s.trim().isEmpty() || !s.trim().startsWith("{")) {
			        finalResponse.put("status", "fail");
			        finalResponse.put("message", "Invalid JSON input.");
			        hasError = true;
			    } else {
			        JSONObject json = new JSONObject(s);
			        String text = json.optString("text", "%");
			        String field = json.optString("field");

			        // Validate 'Name' field input
			        if (field.equalsIgnoreCase("Name")) {
			            if (!text.startsWith("DEV-") || text.contains("*00*")) {
			                finalResponse.put("status", "fail");
			                finalResponse.put("message", "For 'Name' field, the text must start with 'DEV-' and cannot contain '*00*'.");
			                hasError = true;
			            }
			        }

			        // Ensure input text is at least 3 characters long
			        if (text.equals("*") || text.length() < 3) {
			            finalResponse.put("status", "fail");
			            finalResponse.put("message", "Please provide at least 3 characters or digits for the search.");
			            hasError = true;
			        }

			        // Prepare wildcard text
			        text = text.replace("*", "%");

			        if (!hasError) {
						String url = System.getenv("SupplierPortalDBURL");
						String postgresPass = System.getenv("SupplierPortalDBPassword");
						String postgresUser = System.getenv("SupplierPortalDBUsername");

			            try {
			                Class.forName("org.postgresql.Driver");
			                try (Connection con = DriverManager.getConnection(url, postgresUser, postgresPass)) {
			                    String sql = "";

			                    // SQL query based on the input field
			                    if (field.equalsIgnoreCase("ids")) {
			                        sql = "SELECT deviationid, type FROM " + devtablename + " WHERE caid ILIKE ?";
			                    } else if (field.equalsIgnoreCase("name")) {
			                        sql = "SELECT deviationid, name, type FROM " + devtablename + " WHERE name ILIKE ? AND name ILIKE 'DEV-%'";
			                    } else if (field.equalsIgnoreCase("Everything")) {
			                        sql = "SELECT column_name FROM information_schema.columns WHERE table_name = ' " + devtablename + "'";
			                    }

			                    try (PreparedStatement ps = con.prepareStatement(sql)) {
			                        if (field.equalsIgnoreCase("ids") || field.equalsIgnoreCase("name")) {
			                            ps.setString(1, text);  // Set search text
			                        }

			                        ResultSet rs = ps.executeQuery();
			                        Map<String, List<String>> caidsByType = new HashMap<>();

			                        if (field.equalsIgnoreCase("Everything")) {
			                            // Handle 'Everything' query logic
			                            while (rs.next()) {
			                                String columnName = rs.getString("column_name");
			                                String query = "SELECT deviationid, type FROM " + devtablename + " WHERE " + columnName + " ILIKE ?";

			                                try (PreparedStatement ps2 = con.prepareStatement(query)) {
			                                    ps2.setString(1, text);
			                                    ResultSet innerRs = ps2.executeQuery();

			                                    while (innerRs.next()) {
			                                        String caid = innerRs.getString("deviationid");
			                                        String type = innerRs.getString("type");

			                                        // Check for acknowledgment in 'ca_suppliers_details'
			                                        String checkSql = "SELECT COUNT(*) FROM " + suppliersDetailsTable + " WHERE changenumber = ? AND (acknowledge = 'Yes' OR acknowledge = 'No')";
			                                        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
			                                            checkPs.setString(1, caid);
			                                            ResultSet checkSet = checkPs.executeQuery();

			                                            if (checkSet.next() && checkSet.getInt(1) > 0) {
			                                                caidsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(caid);
			                                            }
			                                        }
			                                    }
			                                }
			                            }
			                        } else {
			                            // Handle 'ids' and 'name' field queries
			                            while (rs.next()) {
			                                String caid = rs.getString("deviationid");
			                                String type = rs.getString("type");

			                                String checkSql = "SELECT COUNT(*) FROM " + suppliersDetailsTable + " WHERE changenumber = ? AND (acknowledge = 'Yes' OR acknowledge = 'No')";
			                                try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
			                                    checkPs.setString(1, caid);
			                                    ResultSet checkSet = checkPs.executeQuery();

			                                    if (checkSet.next() && checkSet.getInt(1) > 0) {
			                                        caidsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(caid);
			                                    }
			                                }
			                            }
			                        }

			                        // Construct JSON output in the common format
			                        for (Map.Entry<String, List<String>> entry : caidsByType.entrySet()) {
			                            String type = entry.getKey();
			                            List<String> caids = entry.getValue();

			                            JSONObject typeObject = new JSONObject();
			                            typeObject.put("deviationid", String.join("|", caids)); // Joining caids with '|'

			                            // Create the final object for this type and add it to the results array
			                            JSONObject jsonObject = new JSONObject();
			                            jsonObject.put("type: " + type, typeObject);

			                            jsonArray.put(jsonObject); // Add to the common results array
			                        }

			                        finalResponse.put("results", jsonArray);
			                    }
			                }
			            } catch (Exception e) {
			                e.printStackTrace();
			                finalResponse.put("status", "fail");
			            }
			        }
			    }

			    // Return appropriate response
			    if (hasError) {
			        return Response.status(Response.Status.BAD_REQUEST)
			                .entity(finalResponse.toString())
			                .type(MediaType.APPLICATION_JSON)
			                .build();
			    } else {
			        return Response.ok(finalResponse.toString(), MediaType.APPLICATION_JSON).build();
			    }
			}

    /**
     * Checks the supplier user for a given part ID and retrieves related details.
     *
     * @param uriInfo The URI information containing the query parameters.
     * @return A JSON string containing the supplier details or an error message.
     * @throws Exception if an error occurs during processing.
     */
// Web service method to check if cd.name equals mpn.manufacturername
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

		    // Define SQL query to fetch cd.name and mpn.manufacturername
		    String nameAndManufacturerQuery = "SELECT cd.name, mpn.manufacturername " +
		                                      "FROM mpn_related_parts_details mrpd " +
		                                      "JOIN mpn ON mrpd.mpnid = mpn.mpnid " +
		                                      "JOIN company_details cd ON mpn.manufacturername = cd.name " +
		                                      "JOIN supplier_person_details spd ON cd.companyid = spd.companyid " +
		                                      "WHERE mrpd.partid = ?";

		    // Define a separate SQL query to fetch module_end_item from ec_parts_details
		    String moduleEndItemQuery = "SELECT ec.module_end_item " +
		                                "FROM ec_parts_details ec " +
		                                "WHERE ec.partid = ?";

		    Class.forName("org.postgresql.Driver");

		    // Initialize response variables
		    String cdName = "";
		    String manufacturerName = "";
		    String moduleEndItem = "";
		    boolean isMatch = false;

		    // Connect to the database and execute the first query
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
		            if (cdName.equals(manufacturerName)) {
		                isMatch = true;
		            }
		        }

		        // Set the partId parameter in the second query
		        moduleEndItemStmt.setString(1, partId);
		        ResultSet rsModule = moduleEndItemStmt.executeQuery();

		        // Process the result set for module_end_item
		        if (rsModule.next()) {
		            moduleEndItem = rsModule.getString("module_end_item");
		        }

		    } catch (SQLException e) {
		        e.printStackTrace();
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
