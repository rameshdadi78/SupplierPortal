package Sp.Supplierportal;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//import javax.servlet.http.Part;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class LoginPage {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     * @throws Exception 
     */

	@POST
	@Path("signup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response Signup(String loginDetails) {
	    try {
	        JSONObject json = new JSONObject(loginDetails);

	        // Extract input fields
	        String email = json.optString("email", "").trim();
	        String username = json.optString("username", "").trim();
	        String firstname = json.optString("firstname", "").trim();
	        String lastname = json.optString("lastname", "").trim();
	        String password = json.optString("password", "").trim();
	        String confirmpassword = json.optString("confirmpassword", "").trim();
	        String country = json.optString("country", "").trim();

 // Load properties file
	        Properties pro = new Properties();
	        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	        if (input == null) {
	            throw new FileNotFoundException("sp.properties file not found.");
	        }
	        pro.load(input);

	        String emailDomainNameHere = pro.getProperty("emailDomainName");
	     // Split the domain names and create a regex pattern
	        String[] domains = emailDomainNameHere.split("\\|");
	        String domainPattern = String.join("|", domains).replace(".", "\\."); // Escape dots for regex
	        String emailPattern = "^[a-zA-Z0-9._%+-]+@(" + domainPattern + ")$";
	     // Email validation with example and format check
	        if (email.isEmpty()) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Email is mandatory to create a user. Example: user@example.com\"}")
	                    .build();
	        } else if (!email.matches(emailPattern)) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Invalid email format. Example: user@example.com\"}")
	                    .build();
	        }

	        // Updated Username validation with new regex pattern
	        String usernamePattern = "^[a-zA-Z0-9]([._-]?[a-zA-Z0-9]+){2,19}$";
	        if (username.isEmpty()) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Username is mandatory to create a user. Example: john_doe123\"}")
	                    .build();
	        } else if (!username.matches(usernamePattern)) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Username must start with an alphanumeric character, can include periods, underscores, or hyphens, and be between 5 and 20 characters. Example: john_doe123 or john-doe\"}")
	                    .build();
	        }

	        // Firstname validation with example and alphabetic check
	        if (firstname.isEmpty()) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Firstname is mandatory to create a user. Example: John\"}")
	                    .build();
	        } else if (!firstname.matches("[A-Za-z]+")) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Firstname should only contain alphabetic characters. Example: John\"}")
	                    .build();
	        } else if (firstname.length() < 2) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Firstname must have at least 2 characters. Example: John\"}")
	                    .build();
	        }

	        // Lastname validation with example and alphabetic check
	        if (lastname.isEmpty()) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Lastname is mandatory to create a user. Example: Doe\"}")
	                    .build();
	        } else if (!lastname.matches("[A-Za-z]+")) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Lastname should only contain alphabetic characters. Example: Doe\"}")
	                    .build();
	        } else if (lastname.length() < 2) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Lastname must have at least 2 characters. Example: Doe\"}")
	                    .build();
	        }

	        // Country validation with example and alphabetic/spaces check
	        if (country.isEmpty()) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Country is mandatory to create a user. Example: USA\"}")
	                    .build();
	        } else if (!country.matches("[A-Za-z ]+")) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Country should only contain alphabetic characters and spaces. Example: United States\"}")
	                    .build();
	        }

	        // Password validation with example and pattern check
	        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
	        if (password.isEmpty()) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Password is mandatory to create a user. Example: P@ssw0rd123\"}")
	                    .build();
	        } else if (!password.matches(passwordPattern)) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Password must contain at least one uppercase letter, one lowercase letter, one digit, one special character, and be at least 8 characters long. Example: P@ssw0rd123\"}")
	                    .build();
	        }

	        // Confirm password validation with example
	        if (!password.equals(confirmpassword)) {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("{\"error\": \"Password and Confirm Password do not match. Example: P@ssw0rd123\"}")
	                    .build();
	        }

	        // Email uniqueness check
	        String checkEmailSql = "SELECT COUNT(*) FROM supplierportal_schema1.login_details WHERE email = ?";
	       		String url = System.getenv("SupplierPortalDBURL");
					String pass = System.getenv("SupplierPortalDBPassword");
					String user = System.getenv("SupplierPortalDBUsername");
	        Class.forName("org.postgresql.Driver");
	        try (Connection driver = DriverManager.getConnection(url, user, pass);
	                PreparedStatement checkEmailStmt = driver.prepareStatement(checkEmailSql)) {
	                
	               checkEmailStmt.setString(1, email);
	               ResultSet rs = checkEmailStmt.executeQuery();
	               if (rs.next() && rs.getInt(1) > 0) {
	                   return Response.status(Response.Status.CONFLICT) // 409 Conflict
	                           .entity("{\"error\": \"The Email ID already exists, please try another email ID.\"}")
	                           .build();
	               }
	        }

	        // Password hashing and SQL insertion
	        String bcryptHashed = BCrypt.hashpw(password, BCrypt.gensalt());
	        String sql = "INSERT INTO supplierportal_schema1.login_details (email, username, firstname, lastname, password, confirmpassword, country) "
	                + "VALUES(?, ?, ?, ?, ?, ?, ?)";
	       try (Connection driver = DriverManager.getConnection(url, user, pass);
	             PreparedStatement pstmt = driver.prepareStatement(sql)) {
	            pstmt.setString(1, email);
	            pstmt.setString(2, username);
	            pstmt.setString(3, firstname);
	            pstmt.setString(4, lastname);
	            pstmt.setString(5, bcryptHashed);
	            pstmt.setString(6, bcryptHashed); 
	            pstmt.setString(7, country);
	            pstmt.executeUpdate();



	         // Return success response with the username included
	            return Response.ok("{\"message\": \"User '" + username + "' created successfully\"}").build();

	        }
	    } catch (SQLException e) {

	        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	                .entity("{\"error\": \"Database error: " + e.getMessage() + "\"}")
	                .build();
	    } catch (Exception e) {

	        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	                .entity("{\"error\": \"An unexpected error occurred: " + e.getMessage() + "\"}")
	                .build();
	    }
	}


	
			/**
			* Endpoint for user login.
			*
			* @param loginDetails JSON string containing login credentials.
			* @return Response indicating the result of the login operation.
			* @throws Exception if an error occurs during the login process.
			*/
	@POST
	@Path("login")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(String loginDetails) throws Exception {
		JSONObject js = new JSONObject();

		try {
			JSONObject json = new JSONObject(loginDetails);
			String username = json.getString("username");
			String password = json.getString("password");
			if(username != null && password != null) {
				String sql = "select * from supplierportal_schema1.login_details";
		    	String url = System.getenv("SupplierPortalDBURL");
						String dbPassword = System.getenv("SupplierPortalDBPassword");
						String user = System.getenv("SupplierPortalDBUsername");
				 
				Class.forName("org.postgresql.Driver");
				
				Connection con = DriverManager.getConnection(url, user, dbPassword);
				Statement stmt = con.createStatement();
				ResultSet set = stmt.executeQuery(sql);
				String status = "";

				while(set.next()) {
					String name = set.getString("email");
					String pass = set.getString("password");
					if(name != null && pass != null) {
						if(name.equalsIgnoreCase(username)) {

							boolean ismatch = BCrypt.checkpw(password, pass);

							if(ismatch) {
								status = "successful";
								String jwt = CreateJwt(username);
						    	String sql1 = "update supplierportal_schema1.login_details SET jwt = '"+jwt+"' Where email = '"+username+"'";
								stmt.execute(sql1);
								Map map = addPreference(username);
								JSONObject json1= new JSONObject(map);
								js.put("preference", map);
								js.put("status", status);
								js.put("jwt", jwt);
								return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
							}
							
						}
					}
				}
				status ="unsuccessful";
				js.put("status", status);
			}

		}catch(Exception e) {
			e.printStackTrace();	
		}
		return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();	
	}
	
			/**
			* Endpoint for importing data from a specified file.
			*
			* @param s      JSON string containing the file path.
			* @param headers HTTP headers containing the JWT.
			* @return Response indicating the result of the import operation.
			*/
	@POST
	@Path("import")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertData(String s,@Context HttpHeaders headers) {
		String jwt = headers.getHeaderString("jwt");

		JSONObject json = new JSONObject(s);
		JSONObject js = new JSONObject();
		String filepath = json.getString("filepath");
		boolean check = CheckUser(jwt);

		if(check) {
			String jwts =CheckSessionTime(jwt);
			if(jwts.equalsIgnoreCase("Expired")){
				js.put("jwt", jwts);
				return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
			}
			else {
				File file = new File(filepath);
				try {
					if(file.exists()) {
						FileReader fr = new FileReader(file);
						BufferedReader br = new BufferedReader(fr);
						String header = br.readLine();
						if(header != null) {
							String st;
							while((st = br.readLine()) != null) {
								String[] split = st.split(",");
								String username = split[0];
								String password = split[1];
								if(username != null && password != null) {
									String status = isValidPassword(password);
									if(status.equalsIgnoreCase("ValidPassword")) {
										insertData(username, password);
									}
									else {
										ExportData(username, password, status);
									}
								}
								
							}
								
							js.put("status","Successful");
							js.put("jwt", jwts);
							return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
						}
						js.put("status","fail");
						return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();

					}
					else {
						js.put("status", "FileNotExist");
						return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
					}
					
				}catch(Exception e) {
					e.printStackTrace();
				}
			}

		}
		
		return Response.status(Response.Status.UNAUTHORIZED).build();
	}
	
			/**
			* Endpoint for updating a user's password.
			*
			* @param s      JSON string containing the username and new password.
			* @param headers HTTP headers containing the JWT.
			* @return Response indicating the result of the password update operation.
			*/
	@POST
	@Path("update")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response forgetPassword(String s,@Context HttpHeaders headers) {
		String jwt = headers.getHeaderString("jwt");
		boolean result = CheckUser(jwt);
		if(result) {
			JSONObject json = new JSONObject(s);
			JSONObject js = new JSONObject();
			String username = json.getString("username");
			String newPassword  = json.getString("newPassword");
			String bcryptHashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
			
			String sql = "SELECT * from supplierportal_schema1.login_details";
			String sqls = "UPDATE supplierportal_schema1.login_details SET password = '"+bcryptHashed+"' WHERE name ILIKE '"+username+"' ";
	    			
					String url = System.getenv("SupplierPortalExampleDBURL");
					String postgrespass = System.getenv("SupplierPortalDBPassword");
					String postgresUser = System.getenv("SupplierPortalDBUsername");

	    	
	    	try {
	    		Class.forName("org.postgresql.Driver");
	    		Connection con = DriverManager.getConnection(url, postgresUser, postgrespass);
	    		Statement stmt = con.createStatement();
	    		ResultSet set = stmt.executeQuery(sql);
	    		
	    		while(set.next()) {
	    			String name = set.getString("name");
	    			if(name != null) {
	    				if(username.equalsIgnoreCase(name)) {
	        				stmt.execute(sqls);
	        				js.put("status", "successful");
	        				return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
	        			}
	    			}
	    		}
	    		
	    	}catch(Exception e) {
	    		e.printStackTrace();
	    	}
	    	js.put("status", "unsuccessful");
	    	return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();

		}
		else {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
	}
	
			/**
			* Endpoint for updating user preferences.
			*
			* @param s      JSON string containing the username and new preferences.
			* @param header HTTP headers containing the JWT.
			* @return Response indicating the result of the update operation.
			*/
	@POST
	@Path("updatePre")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updatePreference(String s,@Context HttpHeaders header) {
		String jwt  = header.getHeaderString("jwt");
		boolean result  = CheckUser(jwt);
		if(result) {
			List list = new ArrayList();
			
			JSONObject js = new JSONObject();
			JSONObject json = new JSONObject(s);
			String username = json.getString("username");
			JSONObject preference = json.getJSONObject("preference");

			String view_names = preference.getString("view_name");
			String sql = "select * from supplierportal_schema1.login_details";
			
		String url = System.getenv("SupplierPortalDBURL");
					String password = System.getenv("SupplierPortalDBPassword");
					String user = System.getenv("SupplierPortalDBUsername");
	    	try {
	    		Class.forName("org.postgresql.Driver");
	    		Connection con = DriverManager.getConnection(url, user, password);
	    		Statement stmt = con.createStatement();
	    		ResultSet set = stmt.executeQuery(sql);
	    		String preferences = "";
	    		JSONObject js1 = null ;
	    		while(set.next()) {
	    			String email = set.getString("email");
	    			preferences = set.getString("preferences");
	    			
	    			if(username.equalsIgnoreCase(email)) {
	    				js1 = new JSONObject(preferences);
	    				JSONObject js2 = js1.getJSONObject("views");
	    				JSONArray jsA = js2.getJSONArray("main_table_view");
	    				for(int i = 0; i <= jsA.length()-1 ;i++  ) {
	    		            JSONObject jsonObject = jsA.getJSONObject(i);
	    					String view_name = jsonObject.getString("view_name");
	    					boolean defaults = jsonObject.getBoolean("default");
	    					if(defaults) {
	    						jsonObject.put("default", "false");
	    					}
	    					list.add(view_name);
	    				}
	    				if(list.contains(view_names)) {
	    					System.out.println("Already Present");
	    				}
	    				else {
	    					jsA.put(preference);
	    				}
	    				
	    			}
	    		}
	    		String sql1 = "update supplierportal_schema1.login_details set preferences = '"+js1+"' where email ILIKE '"+username+"'";
				stmt.execute(sql1);
	    		return Response.ok(js1.toString(),MediaType.APPLICATION_JSON).build();
	    		
	    	}catch(Exception e) {
	    		e.printStackTrace();
	    	}
			
	    	js.put("status", "fail");
			return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
		}else {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}		
	}
			/**
			* Endpoint for editing a user's view preferences.
			*
			* @param s      JSON string containing the username and new preference details.
			* @param headers HTTP headers containing the JWT.
			* @return Response indicating the result of the edit operation.
			*/
	@POST
	@Path("edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response editview(String s,@Context HttpHeaders headers) {
		String jwt = headers.getHeaderString("jwt");
		boolean result = CheckUser(jwt);
		if(result) {
			JSONObject jsn =new  JSONObject(s);
			String username  = jsn.getString("username");
			//String preference = jsn.getString("preference");
			JSONObject jsonPrefer = jsn.getJSONObject("preference");
			String viewName = jsonPrefer.getString("view_name");
			
			String sql = "select * from supplierportal_schema1.login_details where email ILIKE '"+username+"'";
			String url = System.getenv("SupplierPortalDBURL");
					String password = System.getenv("SupplierPortalDBPassword");
					String user = System.getenv("SupplierPortalDBUsername");
			
			try {
				Class.forName("org.postgresql.Driver");
				Connection con = DriverManager.getConnection(url, user, password);
				Statement stmt = con.createStatement();
				ResultSet set = stmt.executeQuery(sql);
				JSONObject ob1 = null;
				while(set.next()) {
					String preferences = set.getString("preferences");
					ob1 = new JSONObject(preferences);
					JSONObject ob2 = ob1.getJSONObject("views");
					JSONArray arr = ob2.getJSONArray("main_table_view");
					for(int i = 0;i <= arr.length()-1; i++) {
						JSONObject ob3 = arr.getJSONObject(i);
						String ViewName = ob3.getString("view_name");
						if(viewName.equalsIgnoreCase(ViewName)) {
							arr.remove(i);
							arr.put(jsonPrefer);
						}
					}
				}
				String sql1 = "UPDATE supplierportal_schema1.login_details set preferences = '"+ob1+"' where email ILIKE '"+username+"' ";
				stmt.execute(sql1);
				
				return Response.ok(ob1.toString(),MediaType.APPLICATION_JSON).build();
			}catch(Exception e) {
				e.printStackTrace();
			}
			JSONObject json = new JSONObject();
			json.put("status", "fail");
			return Response.ok(json.toString(),MediaType.APPLICATION_JSON).build();
			
		}else {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
		
	}
	


			/**
			* Endpoint for searching data in the ec_parts_details table based on the specified field and text.
			*
			* @param s JSON string containing the search criteria (text and field).
			* @return Response containing the search results or an error status.
			*/
	@POST
	@Path("search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getData(String s) {
		Map<String,String> map = new HashMap<String, String>();
		Set hSet = new HashSet();
		JSONObject json = new JSONObject(s);
		JSONObject js = new JSONObject();
		JSONArray jsonarray  = new JSONArray();
		String text = json.getString("text");
		String field = json.getString("field");

		if(field.equalsIgnoreCase("Everything")) {
			String sql = "select  column_name,data_type from information_schema.columns where table_name = 'supplierportal_schema1.ec_parts_details'";
			
			String url = System.getenv("SupplierPortalDBURL");
				String password = System.getenv("SupplierPortalDBPassword");
				String user = System.getenv("SupplierPortalDBUsername");

	    	
			try {
				Class.forName("org.postgresql.Driver");
				
				Connection con = DriverManager.getConnection(url, user, password);
				Statement stmt = con.createStatement();
				
				ResultSet set = stmt.executeQuery(sql);
				while(set.next()) {
					String colName = set.getString("column_name");
					String datatype = set.getString("data_type");
					
					map.put(colName, datatype);
				}
				for(String a:map.keySet()) {
					String colName = a;
					String datatype = map.get(a);
					if(datatype.equalsIgnoreCase("character varying") || datatype.equalsIgnoreCase("text") ||
		                    datatype.equalsIgnoreCase("char")) {
						String sql2 = "select * from supplierportal_schema1.ec_parts_details where "+colName+" ILIKE '%"+text+"%'";
						ResultSet set2 = stmt.executeQuery(sql2);
						while(set2.next()) {
							String id = set2.getString("id");
							hSet.add(id);
						}
					}
				}
				JSONArray ja = new JSONArray(hSet);
				js.put("id", ja);
				return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		else if(field.equalsIgnoreCase("name")) {
			String sql = "select * from supplierportal_schema1.ec_parts_details where name ILIKE '%"+text+"%';";
			
			String url = System.getenv("SupplierPortalDBURL");
				String password = System.getenv("SupplierPortalDBPassword");
				String user = System.getenv("SupplierPortalDBUsername");
	    	
	    	try {
	    		Class.forName("org.postgresql.Driver");
	    		
	    		Connection con = DriverManager.getConnection(url, user, password);
	    		Statement stmt = con.createStatement();
	    		
	    		ResultSet set = stmt.executeQuery(sql);
	    		while(set.next()) {
	    			String id = set.getString("id");
	    			jsonarray.put(id);
	    		}
	    		js.put("id",jsonarray);
	    		return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
	    	}catch(Exception e) {
	    		e.printStackTrace();
	    	}
		}
		else if(field.equalsIgnoreCase("description")) {
			String sql = "select * from supplierportal_schema1.ec_parts_details where description ILIKE '%"+text+"%';";
			
				String url = System.getenv("SupplierPortalDBURL");
				String password = System.getenv("SupplierPortalDBPassword");
				String user = System.getenv("SupplierPortalDBUsername");
	    	
	    	try {
	    		Class.forName("org.postgresql.Driver");
	    		
	    		Connection con = DriverManager.getConnection(url, user, password);
	    		Statement stmt = con.createStatement();
	    		
	    		ResultSet set = stmt.executeQuery(sql);
	    		while(set.next()) {
	    			String id = set.getString("id");
	    			jsonarray.put(id);
	    		}
	    		js.put("id",jsonarray);
	    		return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
	    	}catch(Exception e) {
	    		e.printStackTrace();
	    	}
		}

		js.put("status", "fail");
		return Response.ok(js.toString(),MediaType.APPLICATION_JSON).build();
	}
			
			/**
			* Endpoint to delete a view from the user's preferences.
			*
			* @param s JSON string containing the username and view name to be deleted.
			* @param headers HTTP headers containing the JWT for user authentication.
			* @return Response indicating success or failure of the deletion.
			*/
	@POST
	@Path("deleteView")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteVeiw(String s,@Context HttpHeaders headers) {
		String jwt = headers.getHeaderString("jwt");
		boolean result = CheckUser(jwt);
		if(result) {
			JSONObject json = new JSONObject(s);
			String username = json.getString("username");
			String viewName = json.getString("view_name");
			
			String sql = "select * from supplierportal_schema1.login_details";
			String url = System.getenv("SupplierPortalDBURL");
				String password = System.getenv("SupplierPortalDBPassword");
				String user = System.getenv("SupplierPortalDBUsername");
	    	
	    	try {
	    		Class.forName("org.postgresql.Driver");
	    		Connection con = DriverManager.getConnection(url, user, password);
	    		Statement stmt  = con.createStatement();
	    		ResultSet set = stmt.executeQuery(sql);
	    		JSONObject js1 = null;
	    		while(set.next()) {
	    			String name = set.getString("email");
	    			String preferences = set.getString("preferences");
	    			if(name.equalsIgnoreCase(username)) {
	    				js1 = new JSONObject(preferences);
	    				JSONObject js  = js1.getJSONObject("views");
	    				JSONArray array = js.getJSONArray("main_table_view");
	    				for(int i = 0 ; i <= array.length() - 1; i++) {
	    					JSONObject js2 = array.getJSONObject(i);
	    					String view_name = js2.getString("view_name");
	    					if(view_name.equalsIgnoreCase(viewName)) {
	    						array.remove(i);
	    					}
	    					else {
	    						System.out.println("no element");
	    					}
	    				}
	    				
	    			}
	    		}
	    		String sql1 = "update supplierportal_schema1.login_details set preferences = '"+js1+"' where email ILIKE '"+username+"' ";
	    		stmt.execute(sql1);

				return Response.ok(js1.toString(),MediaType.APPLICATION_JSON).build();

	    	}catch(Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	String status = "fail";
			JSONObject json1  = new JSONObject();
			json1.put("status", status);
			return Response.ok(json1.toString(),MediaType.APPLICATION_JSON).build();
		}
		else {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
			
	}
			/**
			* Endpoint to generate a new JWT for the user.
			*
			* @param headers HTTP headers containing the JWT for user authentication.
			* @param data JSON string containing the username.
			* @return Response containing the new JWT or an unauthorized status.
			*/
	@POST 
	@Path("newjwt")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response newJwt(@Context HttpHeaders headers,String data) {
		String jwt = headers.getHeaderString("jwt");
		boolean result = CheckUser(jwt);
		JSONObject js = new JSONObject(data);
		String username = js.getString("username");
		if(result) {
			
			long expirationTime = System.currentTimeMillis() +1805000; 
			System.out.println(expirationTime);
		    String secretKey = "Xploria-Bangalore";

		    String jwtToken = Jwts.builder()
		            .setExpiration(new Date(expirationTime))
		            .claim("username", username)
		            .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
		            .compact();
		    
		    String sql = "update supplierportal_schema1.login_details set jwt = '"+jwtToken+"' where email ILIKE '"+username+"' ";
			String url = System.getenv("SupplierPortalDBURL");
				String password = System.getenv("SupplierPortalDBPassword");
				String user = System.getenv("SupplierPortalDBUsername");
	    	try {
	    		
	    		Class.forName("org.postgresql.Driver");
	    		Connection con = DriverManager.getConnection(url, user, password);
	    		Statement stmt  = con.createStatement();
	    		stmt.execute(sql);
	    		
	    		JSONObject json = new JSONObject();
			    json.put("jwt", jwtToken);

			    return Response.ok(json.toString(),MediaType.APPLICATION_JSON).build();
	    	}catch(Exception e) {
	    		System.out.println(e.getMessage());
	    		e.printStackTrace();
	    	}

			
		}else {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
		return  null;
				
	}
	
			/**
			* Checks the validity of a JWT token and verifies the associated user.
			*
			* @param token the JWT token to validate
			* @return true if the token is valid and the user exists; false otherwise
			*/

	public static boolean CheckUser(String s) {
		String secretKey = "Xploria-Bangalore";
		String username = "";
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey.getBytes()) 
                    .parseClaimsJws(s)
                    .getBody();
            username = (String) claims.get("username");
            Date expiration = claims.getExpiration();
            
            if (expiration.before(new Date())) {
                System.out.println("Token has expired.");
            } else {
                System.out.println("Token is valid. Decoded payload: " + claims);
            }

        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
        }
		String sql = "select * from supplierportal_schema1.login_details";
   	String url = System.getenv("SupplierPortalDBURL");
				String password = System.getenv("SupplierPortalDBPassword");
				String user = System.getenv("SupplierPortalDBUsername");
    	
    	try {
    		Class.forName("org.postgresql.Driver");
    		
    	Connection con = DriverManager.getConnection(url, user, password);
    		Statement stmt = con.createStatement();
    		
    		ResultSet set = stmt.executeQuery(sql);
    		
    		while(set.next()) {
    			String name = set.getString("email");
    			if(name != null) {
        			if(username.trim().equals(name.trim())) {
        				return true;
        			}
        				
    			}
    			
    		}
    		
    	}catch(Exception e) {
    		e.printStackTrace();
    	    	}
	
		return false;
	}
	
	
			/**
			* Creates a JWT token for the specified username with a set expiration time.
			*
			* @param username the username for which to create the JWT
			* @return the generated JWT token as a string
			*/
	public static String CreateJwt(String username) {
		
		long expirationTime = System.currentTimeMillis() +1805000;

	    String secretKey = "Xploria-Bangalore";

	    String jwtToken = Jwts.builder()
	            .setExpiration(new Date(expirationTime))
	            .claim("username", username)
	            .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
	            .compact();

	    return jwtToken;
		
	}

			/**
			* Exports user data (username, password, and status) to a specified file.
			*
			* @param username the username to export
			* @param password the password to export
			* @param status   the status to export
			*/
	public static void ExportData(String username,String password,String status) {
		String filepath = "C:\\Users\\LENOVO\\Desktop\\statusFile.txt";
		File file = new File(filepath);
		if(file.exists()) {

			try {
				FileWriter fw = new FileWriter(file,true);
				BufferedWriter bw = new BufferedWriter(fw);
				
				fw.write(username);
				fw.write("\t");
				fw.write(password);
				fw.write("\t");
				fw.write(status);
				
				fw.write("\n");
				
				
				fw.close();
				System.out.println("Data Added");
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				file.createNewFile();
				System.out.println("new File Created");
				FileWriter fw = new FileWriter(file,true);
				BufferedWriter bw = new BufferedWriter(fw);
				
				bw.write(username);
				bw.write("\t");
				bw.write(password);
				bw.write("\t");
				bw.write(status);
				
				bw.newLine();
				
				bw.close();
				System.out.println("data Added");
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	public static void insertData(String username, String password) {
		Set hset = new HashSet();
		String bcryptHashed = BCrypt.hashpw(password, BCrypt.gensalt());
		
		String sql = "SELECT * from supplierportal_schema1.login_details";
		String sqls = " INSERT INTO supplierportal_schema1.login_details (email,password) VALUES('"+username+"','"+bcryptHashed+"') ";
		String sqles ="UPDATE supplierportal_schema1.login_details SET password = '"+bcryptHashed+"' WHERE email = '"+username+"' ";
    String url=System.getenv("SupplierPortalDBURL");
					String pass=System.getenv("SupplierPortalDBPassword");
					String user= System.getenv("SupplierPortalDBUsername");
		
    	try {
    		Class.forName("org.postgresql.Driver");
    		
    	Connection con = DriverManager.getConnection(url, user, pass);
    		Statement stmt = con.createStatement();	
    		ResultSet set =  stmt.executeQuery(sql);
    		
    		while(set.next()) {
				String name = set.getString("email");
				hset.add(name);
			}
			if(hset.contains(username)) {
				String status = "Updated";
				ExportData(username, password, status);
				stmt.execute(sqles);
			}
			else {
				String status = "success";
				ExportData(username, password, status);
				stmt.execute(sqls);
			}
    		
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
	}
	public static String isValidPassword(String password) {
		if(password.length() < 8) {
			return "password should contain minimum of 8 characters";
		}
		else if(!password.matches(".*[A-Z].*")) {
			return "password should contain atleast 1 uppercase letter";
		}
		else if(!password.matches(".*[a-z].*")) {
			return "password should contain atleast 1 lowercase letter";
		}
		else if(!password.matches(".*\\d.*")) {
			return "password should contain atleast 1 numeric";
		}
		else if(!password.matches(".*[^A-Za-z0-9].*")) {
			return "password should contain atleast 1 special Character";
		}
		return "ValidPassword";
	}
	
	public static Map addPreference(String s) {
        Map<String, Object> map = getDataFromFile();
		String sql = "select preferences from supplierportal_schema1.login_details where email ILIKE '"+s+"' ";
		String sql1 = " ";
	String url=System.getenv("SupplierPortalDBURL");
					String password=System.getenv("SupplierPortalDBPassword");
					String user= System.getenv("SupplierPortalDBUsername");
    	String preference = null;
    	try {
    		Class.forName("org.postgresql.Driver");
    		Connection con = DriverManager.getConnection(url, user, password);
    		Statement stmt = con.createStatement();
    		ResultSet set = stmt.executeQuery(sql);
    		
    		while(set.next()) {
    			preference = set.getString("preferences");
    		}									
    		if(preference == null) {
				JSONObject json = new JSONObject(map);
    			sql1 = "update supplierportal_schema1.login_details set preferences = '"+json+"' where email ILIKE '"+s+"'";
    			stmt.execute(sql1);
    			return map;
			}
    		else {
    			JSONObject json  = new JSONObject(preference);
    			Map<String, Object> dataMap = new HashMap<>();
    			dataMap  = json.toMap();
    			return dataMap;
    		}
			
    		
    	}catch(Exception e){
    		e.printStackTrace();
    	}
		return null;
	}

	
	public static Map<String, Object> getDataFromFile() {
		Map finalMap = new LinkedHashMap();
		Map map = new LinkedHashMap();
		Map map1 = new LinkedHashMap();
		Map map2 = new LinkedHashMap();
		
		List mainTable = new LinkedList();
		List list = new LinkedList();
		List list1 = new LinkedList();
		List list2 = new LinkedList();
		List list3 = new LinkedList();
		List list4 = new LinkedList();
		List list5 = new LinkedList();
	    Properties properties = new Properties();
	    try (InputStream input = LoginPage.class.getClassLoader().getResourceAsStream("properties.properties")) {
			if (input == null) {
		            throw new FileNotFoundException("CAMappingNames.properties file not found.");
		        }
	        properties.load(input);
	        

	        for(String s:properties.stringPropertyNames()) {
	        	String values = properties.getProperty(s);
	        	if(s.startsWith("main_table.")) {
	        		String[] key = s.split("\\.");
	        		if(key[1].equalsIgnoreCase("name")) {
	        			list.add(values);
	        		}
	        		else {
	        			
	        			list1.add(values);
	        		}
	        		map.put("name", list);
	        		map.put("display", list1);
	        		
	        	}
	        	else {
	        		String[] key = s.split("\\.");
	        		if(key[3].equalsIgnoreCase("view_name")) {
	        			map1.put("view_name", values);
	        		}
	        		else if(key[3].equalsIgnoreCase("name")) {
	        			list3.add(values);
	        		}
	        		else if(key[3].equalsIgnoreCase("display")) {
	        			list4.add(values);
	        		}
	        		else if(key[3].equalsIgnoreCase("default")) {
	        			map1.put("default", values);
	        		}
	        		
	        		map1.put("name", list3);
	        		map1.put("display", list4);
	        		
	        		
	        	}
	        	
	        }
	        mainTable.add(map1);
	        map2.put("main_table_view", mainTable);

	        finalMap.put("main_table", map);
	        finalMap.put("views", map2);
	        
	       return finalMap;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	
	
	public static String CheckSessionTime(String s) {
		String[] data = s.split("\\.");
		
		try {
			String decodedData = new String(Base64.getUrlDecoder().decode(data[1]),StandardCharsets.UTF_8);
			JSONObject json = new JSONObject(decodedData);
			String username = json.getString("username");
			String DateAndTime = json.getString("DateAndTime").trim();
			
			DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			LocalDateTime prasedData = LocalDateTime.parse(DateAndTime,format);
			
			LocalDateTime now = LocalDateTime.now();
			
			Duration duration = Duration.between(prasedData, now);
			boolean value = duration.toMinutes() < 30;
			
			if(value) {
				System.out.println("less than 30 mins");
				String newJwt = CreateJwt(username);
				System.out.println("New Jwt is Printed");
				return newJwt;
			}
			
		}catch(Exception e) {
			return "Error";
		}
		return "Expired";
	}

	@Path("userCreation")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response userCreation(@FormDataParam("file") InputStream uploadedInputStream,
	                             @FormDataParam("file") MultiPartFeature asdasdasd,
	                             @FormDataParam("file") MultivaluedMap<String, String> test) {
	    System.out.println("called bulk data creation--");
	   

	    JSONArray userLogs = new JSONArray(); // Create a JSON array to hold user logs

	    try {
	        BufferedReader br = new BufferedReader(new InputStreamReader(uploadedInputStream, "UTF-8"));
	        String line;
	        System.out.println("Header line: " + br.readLine()); // Skip the original header

	        // Add headers to the JSON response
	        JSONObject responseJson = new JSONObject();
	        JSONArray headers = new JSONArray();
	        headers.put("Firstname");
	        headers.put("Lastname");
	        headers.put("ConfirmPassword");
	        headers.put("Password");
	        headers.put("Country");
	        headers.put("Username");
	        headers.put("Description");
	        headers.put("Email");
	        headers.put("Status");

	        responseJson.put("headers", headers);

	        // Database connection setup
		String url=System.getenv("SupplierPortalDBURL");
		String pass=System.getenv("SupplierPortalDBPassword");
		String user= System.getenv("SupplierPortalDBUsername");

	        Class.forName("org.postgresql.Driver");
	       	Connection driver = DriverManager.getConnection(url, user, pass);

	        // Prepare SQL for checking duplicates
	        String checkSql = "SELECT COUNT(*) FROM supplierportal_schema1.login_details WHERE username = ? OR email = ?";
	        PreparedStatement checkStmt = driver.prepareStatement(checkSql);

	        // Loop to read each line of user data
	        while ((line = br.readLine()) != null) {
	            String[] userDetails = line.split("\t"); // Assuming tab-separated values
	            String firstname = userDetails[0].trim();
	            String lastname = userDetails[1].trim();
	            String confirmpassword = userDetails[2].trim();
	            String password = userDetails[3].trim();
	            String country = userDetails[4].trim();
	            String username = userDetails[5].trim();
	            String description = userDetails[6].trim(); // Optional field
	            String email = userDetails[7].trim();

	            // Create a JSON object for each user's details
	            JSONObject userLog = new JSONObject(); // Create a JSON object for user data
	            userLog.put("Firstname", firstname);
	            userLog.put("Lastname", lastname);
	            userLog.put("ConfirmPassword", confirmpassword);
	            userLog.put("Password", password);
	            userLog.put("Country", country);
	            userLog.put("Username", username);
	            userLog.put("Description", description);
	            userLog.put("Email", email);

	            String status = "SUCCESS"; // Default status

		        Properties pro = new Properties();
		        InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		        if (input == null) {
		            throw new FileNotFoundException("sp.properties file not found.");
		        }
		        pro.load(input);

		        String emailDomainNameHere = pro.getProperty("emailDomainName");
		     // Split the domain names and create a regex pattern
		        String[] domains = emailDomainNameHere.split("\\|");
		        String domainPattern = String.join("|", domains).replace(".", "\\."); // Escape dots for regex
		        String emailPattern = "^[a-zA-Z0-9._%+-]+@(" + domainPattern + ")$";
		        
	            if (firstname.isEmpty()) {
	                status = "ERROR: First name is mandatory to create a user.";
	            } else if (!firstname.matches("[A-Za-z]+")) {
	                status = "ERROR: First name can only contain letters.";
	            } else if (firstname.length() < 2) {
	                status = "ERROR: First name must be at least 2 characters long.";
	            }

	            if (lastname.isEmpty()) {
	                status = "ERROR: Last name is mandatory to create a user.";
	            } else if (!lastname.matches("[A-Za-z]+")) {
	                status = "ERROR: Last name can only contain letters.";
	            } else if (lastname.length() < 2) {
	                status = "ERROR: Last name must be at least 2 characters long.";
	            }
	            
	            
	            
	            
	            
	            if (email.isEmpty()) {
	                status = "ERROR: Email is mandatory to create a user.";
	            } else if (!email.matches(emailPattern)) {
	                status = "ERROR: Invalid email format.";
	            } else {
	                String usernamePattern = "^[a-zA-Z0-9]([._-]?[a-zA-Z0-9]+){2,19}$";
	                if (username.isEmpty()) {
	                    status = "ERROR: Username is mandatory to create a user.";
	                } else if (!username.matches(usernamePattern)) {
	                    status = "ERROR: Invalid username format.";
	                } else {
	                    // Check for duplicate username or email
	                    checkStmt.setString(1, username);
	                    checkStmt.setString(2, email);
	                    ResultSet rs = checkStmt.executeQuery();

	                    if (rs.next() && rs.getInt(1) > 0) {
	                        status = "ERROR: Duplicate username  found.";
	                    } else {
	                        // Hash the password using BCrypt
	                        String bcryptHashed = BCrypt.hashpw(password, BCrypt.gensalt());

	                        // Prepare SQL insert query
	                        String sql = "INSERT INTO supplierportal_schema1.login_details (email, username, firstname, lastname, password, country, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
	                        try (PreparedStatement insertStmt = driver.prepareStatement(sql)) {
	                            insertStmt.setString(1, email);
	                            insertStmt.setString(2, username);
	                            insertStmt.setString(3, firstname);
	                            insertStmt.setString(4, lastname);
	                            insertStmt.setString(5, bcryptHashed);
	                            insertStmt.setString(6, country);
	                            insertStmt.setString(7, description);

	                            // Execute the insert query
	                            insertStmt.executeUpdate();
	                        } catch (SQLException e) {
	                            status = "ERROR: " + e.getMessage();
	                        }
	                    }
	                }
	            }

	            userLog.put("Status", status); // Add status to the JSON object
	            userLogs.put(userLog); // Add user log object to JSON array
	        }

	        br.close(); // Close the reader
	        checkStmt.close(); // Close the duplicate check statement
	        driver.close(); // Close the database connection

	        responseJson.put("data", userLogs); // Add user data to the response

	        // Log the final output
	        System.out.println("Final JSON Output: " + responseJson.toString(2));

	        return Response.ok(responseJson.toString(), MediaType.APPLICATION_JSON).build(); // Return JSON response

	    } catch (Exception e) {
	        e.printStackTrace();
	        return Response.serverError()
	                .entity("{\"error\":\"An unexpected error occurred during user creation.\"}").build();
	    }
	}


}
