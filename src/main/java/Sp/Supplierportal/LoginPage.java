package Sp.Supplierportal;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.ResultSet;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
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
    public String Signup(String loginDetails) {
		
    	try {
    		JSONObject json = new JSONObject(loginDetails);
    		String user = json.getString("username");
    		String pass = json.getString("password"); 
            String bcryptHashed = BCrypt.hashpw(pass, BCrypt.gensalt());

            
    		String sql = " INSERT INTO login_details (email,password) VALUES('"+user+"','"+bcryptHashed+"') ";
        	
        	
        	String url=System.getenv("SupplierPortalDBURL");
    		String password=System.getenv("SupplierPortalDBPassword");
    		String username= System.getenv("SupplierPortalDBUsername");
    		
    		Class.forName("org.postgresql.Driver");
    		
    		Connection driver = DriverManager.getConnection(url, username, password);
    		Statement stmt = driver.createStatement();
    		
    		stmt.execute(sql);
    		
    		System.out.println("Data inserted successfully");
    		
    	}catch(Exception e) {
    		return "unsuccessful";
    	}
 
    	return "successful";
    }
	@POST
	@Path("login")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(String loginDetails) throws Exception {
		JSONObject js = new JSONObject();
		System.out.println("called");
		try {
			JSONObject json = new JSONObject(loginDetails);
			String username = json.getString("username");
			String password = json.getString("password");
			if(username != null && password != null) {
				String sql = "select * from login_details";
				String url=System.getenv("SupplierPortalDBURL");
	    		String password1=System.getenv("SupplierPortalDBPassword");
	    		String user= System.getenv("SupplierPortalDBUsername");
		    	
				 
				Class.forName("org.postgresql.Driver");
				
				Connection con = DriverManager.getConnection(url, user, password1);
				Statement stmt = con.createStatement();
				ResultSet set = stmt.executeQuery(sql);
				String status = "";

				while(set.next()) {
					String name = set.getString("email");
					String pass = set.getString("password");
					if(name != null && pass != null) {
						if(name.equalsIgnoreCase(username)) {
							System.out.println(name+" "+username);
							boolean ismatch = BCrypt.checkpw(password, pass);
							System.out.println(ismatch);
							if(ismatch) {
								status = "successful";
								String jwt = CreateJwt(username);
						    	String sql1 = "update login_details SET jwt = '"+jwt+"' Where email = '"+username+"'";
								stmt.execute(sql1);
								Map map = addPreference(username);
								System.out.println("loginPAge"+" "+map);
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
	
	@POST
	@Path("import")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertData(String s,@Context HttpHeaders headers) {
		String jwt = headers.getHeaderString("jwt");
		System.out.println(jwt);
		JSONObject json = new JSONObject(s);
		JSONObject js = new JSONObject();
		String filepath = json.getString("filepath");
		boolean check = CheckUser(jwt);
		System.out.println(check);
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
			
			String sql = "SELECT * from logindetails";
			String sqls = "UPDATE logindetails SET password = '"+bcryptHashed+"' WHERE name ILIKE '"+username+"' ";
	    	
	    	String url=System.getenv("SupplierPortalExampleDBURL");
    		String postgrespass=System.getenv("SupplierPortalDBPassword");
    		String postgresUser= System.getenv("SupplierPortalDBUsername");
	    	
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
			System.out.println(preference);
			String view_names = preference.getString("view_name");
			String sql = "select * from login_details";
			
		   	String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String user= System.getenv("SupplierPortalDBUsername");
			
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
	    		String sql1 = "update login_details set preferences = '"+js1+"' where email ILIKE '"+username+"'";
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
			
			String sql = "select * from login_details where email ILIKE '"+username+"'";
		   	String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String user= System.getenv("SupplierPortalDBUsername");
			
			try {
				Class.forName("org.postgresql.Driver");
				Connection con = DriverManager.getConnection(url,user, password);
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
				String sql1 = "UPDATE login_details set preferences = '"+ob1+"' where email ILIKE '"+username+"' ";
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
			String sql = "select  column_name,data_type from information_schema.columns where table_name = 'ec_parts_details'";
			
		   	String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String user= System.getenv("SupplierPortalDBUsername");
	    	
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
						String sql2 = "select * from ec_parts_details where "+colName+" ILIKE '%"+text+"%'";
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
			String sql = "select * from ec_parts_details where name ILIKE '%"+text+"%';";
			
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
	    	
	    	try {
	    		Class.forName("org.postgresql.Driver");
	    		
	    		Connection con = DriverManager.getConnection(url, userName, password);
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
			String sql = "select * from ec_parts_details where description ILIKE '%"+text+"%';";
			
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
	    	try {
	    		Class.forName("org.postgresql.Driver");
	    		
	    		Connection con = DriverManager.getConnection(url, userName, password);
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
			
			String sql = "select * from login_details";
		   	String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String user= System.getenv("SupplierPortalDBUsername");
	    	
	    	try {
	    		Class.forName("org.postgresql.Driver");
	    		Connection con  = DriverManager.getConnection(url, user, password);
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
	    		String sql1 = "update login_details set preferences = '"+js1+"' where email ILIKE '"+username+"' ";
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
		    
		    String sql = "update login_details set jwt = '"+jwtToken+"' where email ILIKE '"+username+"' ";
		   	String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String user= System.getenv("SupplierPortalDBUsername");
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
            System.out.println("Token Expiry: " + expiration);
            if (expiration.before(new Date())) {
                System.out.println("Token has expired.");
            } else {
                System.out.println("Token is valid. Decoded payload: " + claims);
            }

        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
        }
		String sql = "select * from login_details";
	   	String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String user= System.getenv("SupplierPortalDBUsername");
    	
    	try {
    		Class.forName("org.postgresql.Driver");
    		
    		Connection con = DriverManager.getConnection(url,user,password);
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
		
		String sql = "SELECT * from login_details";
		String sqls = " INSERT INTO login_details (email,password) VALUES('"+username+"','"+bcryptHashed+"') ";
		String sqles ="UPDATE login_details SET password = '"+bcryptHashed+"' WHERE email = '"+username+"' ";
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
        System.out.println("perference"+" "+map);
		String sql = "select preferences from login_details where email ILIKE '"+s+"' ";
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
    			System.out.println("called");
				JSONObject json = new JSONObject(map);
    			sql1 = "update login_details set preferences = '"+json+"' where email ILIKE '"+s+"'";
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
	    String filePath = "C:\\Users\\dell\\Pictures\\new\\Supplierportal\\src\\main\\resources\\properties.properties";
	    Properties properties = new Properties();
	    try {
	        FileInputStream fileInputStream = new FileInputStream(new File(filePath));
	        properties.load(fileInputStream);
	        fileInputStream.close();

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
	        System.out.println(finalMap);
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
}
