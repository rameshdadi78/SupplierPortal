package Sp.Supplierportal_DB;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.*;
import java.io.*;
import java.sql.Timestamp;
import org.mindrot.jbcrypt.BCrypt;

public class InsertIntoPostgres {
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private static final int MIN_RELEASED_STATES = 5;
    private static final int OBJECTS_COUNT=500;
    private static int stateCounter = 0;
    
    private static final int MIN_YES = 5;
    private static int yesCounter = 0;
    
    static ArrayList<String> alPartIds = new ArrayList<>();
    static ArrayList<String> alMPNIds = new ArrayList<>();
    static ArrayList<String> alSpecIds = new ArrayList<>();
    static ArrayList<String> alPartNames = new ArrayList<>();
    private static final Map<String, String> idNameMapPart = new HashMap();
    private static final Map<String, Object> partRow = new HashMap<>();
    
    static ArrayList<String> alCaIds = new ArrayList<>();
    static ArrayList<String> alCaNames = new ArrayList<>();
    private static final Map<String, ArrayList<ArrayList<String>>> changeActionPartDetailsMap = new LinkedHashMap<>();
    private static final Map<String, String> changeActionReasons = new HashMap<>();
    
    static ArrayList<String> devIds = new ArrayList<>();
    private static ArrayList<ArrayList<String>> partDetailsList = new ArrayList<>();
    private static Map<String, ArrayList<String>> deviationPartMap = new LinkedHashMap<>();
    
    private static Map<String, Object> supplierInfo = new HashMap<>();
    private static Map<String, Object> companyInfo = new HashMap<>();
    
    static ArrayList<String> alCompanyIds = new ArrayList<>();
    static ArrayList<String> alCompanyNames = new ArrayList<>();
    private static final Map<String, String> companyIdNameMap = new HashMap();
    private static final Map<String, String> companyIdPersonIdMap = new HashMap();
    private static final Map<String, String> companyNamePersonNameMap = new HashMap();
    private static ArrayList<String> companyNameList  = new ArrayList();
    private static ArrayList<String> caIdList  = new ArrayList();
    private static ArrayList<String> caNameList  = new ArrayList();
    private static ArrayList<ArrayList<String>> personCompanyDetailsList = new ArrayList<>();
    private static ArrayList<String> companyIdList = new ArrayList<>();
    private static Map<String, ArrayList<String>> personCompanyMap = new LinkedHashMap<>();
    
    private static final Map<String, Object> caRow = new HashMap<>();
    private static final Map<String, Object> devRow = new HashMap<>();
    private static ArrayList<ArrayList<String>> caDetailsList = new ArrayList<>();
    private static ArrayList<ArrayList<String>> devDetailsList = new ArrayList<>();
    
    private static final Map<String, Object> supplierRow = new HashMap<>();
    private static ArrayList<ArrayList<String>> supCountList = new ArrayList<>();
    private static HashMap<ArrayList<String>, Integer> supMap = new HashMap<>();
    
    public static void main(String[] args) throws SQLException {
        String filePath = "ec_part.properties";
        Properties properties = new Properties();
        Connection connection = null;

        try (InputStream input = InsertIntoPostgres.class.getClassLoader().getResourceAsStream(filePath)) {
            if (input == null) {
                throw new FileNotFoundException(filePath + " file not found.");
            }
            properties.load(input);

            // Prepare a LinkedHashMap to hold the property keys and values
            LinkedHashMap<String, String> propertiesMap = new LinkedHashMap<>();
            for (String key : properties.stringPropertyNames()) {
                propertiesMap.put(key, properties.getProperty(key));
			}

            String url=System.getenv("SupplierPortalDBURL");
            String password=System.getenv("SupplierPortalDBPassword");
            String userName = System.getenv("SupplierPortalDBUsername");
            
            // Establish a connection to the PostgreSQL db
            Class.forName("org.postgresql.Driver");
            
            connection = DriverManager.getConnection(url, userName , password);
            
            for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
            	String key = entry.getKey();
                String columns = entry.getValue();
                String tableName = "";
                
                if (columns != null && !columns.isEmpty()) {
                    String[] columnArray = columns.split(",");
                    if (key.equals("ec_parts.table.columns")) {
                        tableName = "supplierportal_schema1.ec_parts_details";
                    } else if (key.equals("mpn_details.table.columns")) {
                        tableName = "supplierportal_schema1.mpn_details";
                    } else if (key.equals("specifications_details.table.columns")) {
                        tableName = "supplierportal_schema1.specifications_details";
                    }else if (key.equals("change_action_details.table.columns")) {
//                        tableName = "supplierportal_schema1.change_action_details";
                    	createChangeActionDetails(connection, propertiesMap);
                    	continue;
                    }else if (key.equals("deviation_details.table.columns")) {
                        tableName = "supplierportal_schema1.deviation_details";
                    }else if (key.equals("company_details.table.columns")) {
                    	createCompanyDetails(connection, propertiesMap);
                    	continue;
                    }else if (key.equals("supplier_person_details.table.columns")) {
                    	createSupplierPersonDetails(connection, propertiesMap);
                        continue;
                    }else if (key.equals("country_details.table.columns")) {
                        tableName = "supplierportal_schema1.country_details";
                    }else if (key.equals("mpn_related_parts_details")) {
                        continue;
                    } else if (key.equals("specifications_related_objects.table.columns")) {
                        continue;
                    }else if (key.equals("change_action_affected_items.table.columns")) {
                        continue;
                    }else if (key.equals("deviation_Reported_Against.table.column")) {
                        continue;
                    }
                    else if (key.equals("ebom_details.table.columns")) {
                        continue;
                    }else if (key.equals("supplier_person_member_company_details.table.columns")) {
                        continue;
                    }else if (key.equals("alternate_part_details.table.columns")) {
                        continue;
                    }else if (key.equals("last_processed_details.table.columns")) {
                        continue;
                    }else if (key.equals("ca_deviation_suppliers_details.table.columns")) {
                        continue;
                    }else if (key.equals("ec_parts_last_released.table.columns")) {
                        continue;
                    }else if (key.equals("supplier_counts.table.columns")) {
                        continue;
                    }else if (key.equals("login_details.table.columns")) {
                        continue;
                    }
                     // To construct the SQL INSERT statement dynamically
                    String placeholders = generatePlaceholders(columnArray.length);
                    String insertSQL = String.format(
                        "INSERT INTO %s (%s) VALUES (%s);",
                        tableName,
                        String.join(",", columnArray),
                        placeholders
                    );

                    PreparedStatement preparedStatement = null;
                    try {
                        preparedStatement = connection.prepareStatement(insertSQL);

                        // Generate and insert rows of data according to OBJECTS_COUNT
                        for (int i = 1; i <= OBJECTS_COUNT; i++) {
                            setValues(preparedStatement, columnArray, i, key);
                            int rowsInserted = preparedStatement.executeUpdate();
                        }
                    } finally {
                        // Close the prepared statement after each table
                        if (preparedStatement != null) preparedStatement.close();
                    }
                } else {
                    System.out.println("No columns found for key: " + key);
                }
            }
            

            createPartMPNRelationships(connection, propertiesMap);

            // Create Part-Specification relationships
            createPartSpecificationRelationships(connection, propertiesMap);

            // Create Change Action Affected Items
            createChangeActionAffectedItems(connection, propertiesMap);
            
            associateDeviationsWithPartDetails();
            createDeviationReportedAgainst(connection, propertiesMap);
            
            createEBOMConnections(connection, propertiesMap);
            
            createSupplierPersonMemberCompany(connection, propertiesMap);
            
            createAlternatePartDetails(connection, propertiesMap);
            
            createLastProcessedDetails(connection, propertiesMap);
            
            createCaDeviationSuppliersDetails(connection, propertiesMap);
            
            createPartLastReleasedDetails(connection, propertiesMap);
            
            createSupplierCountsDetails(connection, propertiesMap);
            
            createLoginDetails(connection, propertiesMap);

//            input.close();
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) connection.close();
        }
        System.out.println("Done");    
        }

    //method to generate placeholders for the PreparedStatement
    private static String generatePlaceholders(int count) {
        return "?" + ",?".repeat(Math.max(0, count - 1));
    }

    // method to set values for each column
    private static void setValues(PreparedStatement ps, String[] columnNames, int sequenceNumber, String key) throws SQLException {
        String partId = null;
        String mpnId = null;
        String specId = null;
        String partName = null;
        String partCA = null;


        String sPartId = "";
        String sPartName = "";

        
        boolean isDeviationRow = false;
        boolean isPartReleased = false;
        for (int i = 0; i < columnNames.length; i++) {
            String columnName = columnNames[i];
            Object value = generateValue(columnName, sequenceNumber, key);
            ps.setObject(i + 1, value);

            if (key.equals("deviation_details.table.columns")) {
            	devRow.put(columnName, value);
                if (columnName.equals("Reported_Against") && "Yes".equals(value)) {
                    isDeviationRow = true;
                }
            }
            
            if (key.equals("change_action_details.table.columns")) {
            	if (columnName.equals("ChangeNumber")) {
                    partCA = (String) value;
                    alCaIds.add(partCA);
                }
            	caRow.put(columnName, value);

            }
            
            // to process ec_parts.table.columns
            if (key.equals("ec_parts.table.columns")) {
                partRow.put(columnName, value);
                if (columnName.equals("State") && "Released".equals(value)) {
                    isPartReleased = true;
                }

                if (columnName.equals("Id")) {
                    partId = (String) value;
                    alPartIds.add(partId);
                } else if (columnName.equals("Name")) {
                    partName = (String) value;
                    alPartNames.add(partName);
                }
            }
            
            // for other tables
            if (columnName.equals("Id") && key.equals("specifications_details.table.columns")) {
                specId = (String) value;
                alSpecIds.add(specId);
            } else if (columnName.equals("MPNId")) {
                mpnId = (String) value;
                alMPNIds.add(mpnId);
            }
        }

        if (key.equals("deviation_details.table.columns") && isDeviationRow) {
            processDeviationRow(devRow);
        }

        // process part row if released
        if (key.equals("ec_parts.table.columns") && isPartReleased) {
            processPartRow(partRow);
        }
        
        if (key.equals("change_action_details.table.columns")) {
        	processCaRow(caRow);
        }
        
        if (key.equals("deviation_details.table.columns")) {
            processDevRow(devRow);
        }

        if (key.equals("ec_parts.table.columns")) {
            processPartCAsRow(partRow);
        }
        		
        //logic for parts
        for (int j = 0; j < alPartIds.size(); j++) {
            if (alPartIds.size() > j && alPartNames.size() > j) {
                sPartId = (String) alPartIds.get(j);
                sPartName = (String) alPartNames.get(j);
                idNameMapPart.put(sPartId, sPartName);
            }
        }
    }

 // process Deviation Rows
    private static void processDeviationRow(Map<String, Object> deviationRow) {
        String devid = (String) deviationRow.get("DeviationId");
        String reportedAgainst = (String) deviationRow.get("Reported_Against");
        devIds.add(devid);
    }

    // process Part Rows
    private static void processPartRow(Map<String, Object> partRow) {
        String id = (String) partRow.get("Id");
        String name = (String) partRow.get("Name");

        ArrayList<String> partDetails = new ArrayList<>();
        partDetails.add(id);
        partDetails.add("Part");
        partDetails.add(name);
        partDetails.add("01");
        partDetailsList.add(partDetails);
    }
    
    private static void processCaRow(Map<String, Object> caRow) {
    	String changeNum = (String) caRow.get("ChangeNumber");
        String name = (String) caRow.get("Name");

        ArrayList<String> caDetails = new ArrayList<>();
        caDetails.add(changeNum);
        caDetails.add("Change Action");
        caDetails.add(name);
        caDetails.add("01");
        caDetailsList.add(caDetails);
        
    }
    
    private static void processDevRow(Map<String, Object> devRow) {
        String devNum = (String) devRow.get("DeviationId");
        String devName = (String) devRow.get("Name");

        ArrayList<String> devDetails = new ArrayList<>();
        devDetails.add(devNum);
        devDetails.add("Deviation");
        devDetails.add(devName);
        devDetails.add("01");
        devDetailsList.add(devDetails);
    }
    
    private static void processPartCAsRow(Map<String, Object> partRow) {
        String changeActioNumber= (String) partRow.get("ChangeNumber");
        String id = (String) partRow.get("Id");
        String name = (String) partRow.get("Name");
        String[] caArray = changeActioNumber.split("\\|");
        for(String caNumber : caArray) {
        	ArrayList<String> partDetails = new ArrayList<>();
            partDetails.add(id);
            partDetails.add("Part");
            partDetails.add(name);
            partDetails.add("01");
            changeActionPartDetailsMap.putIfAbsent(caNumber, new ArrayList<>());
            changeActionPartDetailsMap.get(caNumber).add(partDetails);
            
        }
    }
    
    private static Object generateValue(String columnName, int sequenceNumber, String key) {
        Random random = new Random();
        String randomCompanyId = "";
        int areaCode = 200 + random.nextInt(800);
        int middlePart = 100 + random.nextInt(900);
        int lastPart = 1000 + random.nextInt(9000);
        switch (columnName) {
            case "Id":
            case "MPNId":
            case "DeviationId":
            case "PersonId":
            case "CountryId":
                return String.format("00001.00001.00001.%05d", idCounter.incrementAndGet());

            case "ChangeNumber":
            	if (key.equals("ec_parts.table.columns")) {
            		return getRandomValues(caIdList, 3);
                }else if (key.equals("change_action_details.table.columns")) {
            		return String.format("00001.00001.00001.%05d", idCounter.incrementAndGet());
            	}
            case "Type":
                if (key.equals("ec_parts.table.columns")) {
                    return "Part";
                }else if (key.equals("mpn_details.table.columns")) {
                    return "MPN";
                }else if (key.equals("specifications_details.table.columns")) {
                    return "Part Specification";
                }else if (key.equals("change_action_details.table.columns")) {
                    return "Change Action";
                }else if (key.equals("deviation_details.table.columns")) {
                    return "Deviation";
                }else if (key.equals("deviation_details.table.columns")) {
                    return "Deviation";
                }else if (key.equals("company_details.table.columns")) {
                    return "Company";
                }else if (key.equals("supplier_person_details.table.columns")) {
                    return "Supplier-Person";
                }else {
                    return "Unknown Type";
                }
            case "Name":
                if (key.equals("ec_parts.table.columns")) {
                    return String.format("%07d-00-A", sequenceNumber);
                } else if (key.equals("mpn_details.table.columns")) {
                    return String.format("%07d-00-A", sequenceNumber);
                } else if (key.equals("specifications_details.table.columns")) {
                    return String.format("%07d-00-A", sequenceNumber);
                }else if (key.equals("change_action_details.table.columns")) {
                	return String.format("CA-%07d", sequenceNumber);
                } else if (key.equals("deviation_details.table.columns")) {
                	return String.format("DEV-%07d", sequenceNumber);
                }else if (key.equals("company_details.table.columns")) {
                	return String.format("COM-%07d", sequenceNumber);
                }else if (key.equals("supplier_person_details.table.columns")) {
                	return String.format("SUP-%07d", sequenceNumber);
                }else if (key.equals("country_details.table.columns")) {
                	return String.format("CNTRY-%07d", sequenceNumber);
                }else {
                    return "Unknown Type";
                }
            case "MPNDetails":
            	return String.format("%07d-00-A", sequenceNumber);
            case "CompanyName":
            	if (key.equals("mpn_details.table.columns")) {
            		return companyNameList.get(random.nextInt(companyNameList.size()));
            	}
            case "Rev":
            	if (key.equals("ec_parts.table.columns")) {
                    return "01";
                } else if (key.equals("mpn_details.table.columns")) {
                    return "01";
                } else if (key.equals("specifications_details.table.columns")) {
                    return "01";
                }else if (key.equals("change_action_details.table.columns")) {
                    return "-";
                }else if (key.equals("company_details.table.columns")) {
                    return "~";
                }else if (key.equals("supplier_person_details.table.columns")) {
                    return "~";
                }else {
                    return "-";
                }
            case "Policy":
            	if (key.equals("ec_parts.table.columns")) {
                    return "EC Part";
                }else if (key.equals("mpn_details.table.columns")) {
                    return "MPN";
                }else if (key.equals("specifications_details.table.columns")) {
                    return "Part Specification";
                }else if (key.equals("change_action_details.table.columns")) {
                    return "Change Action";
                }else if (key.equals("deviation_details.table.columns")) {
                    return "Deviation";
                }else if (key.equals("company_details.table.columns")) {
                    return "Company";
                }else {
                    return "-";
                }
            case "State":
            	return generateState();
            case "Organization":
                return "Engineering";
            case "Project":
                return "Project" + sequenceNumber;
            case "Owner":
                return "User" + sequenceNumber;
            case "Originated":
            case "Modified":
            case "CurrentStateAttainedDate":
                return LocalDate.now();
            case "ReleaseDate":
                return LocalDate.now().plusMonths(1);
            case "IsLastRevision":
                return true;
            case "Description":
                return "Description for " + (key.equals("mpn_details.table.columns") ? "MPN " : "part ") + sequenceNumber;
            case "Spare_Part":
                return random.nextBoolean();
            case "Part_Classification":
                return "Class" + (sequenceNumber % 3 + 1);
            case "Effectivity_Date":
                return LocalDate.now().plusWeeks(2);
            case "End_Effectivity":
                return LocalDate.now().plusYears(5);
            case "Estimated_Cost":
                return 100 + random.nextDouble() * 900;
            case "Lead_Time":
                return 7 + random.nextInt(14);
            case "Material_Category":
                return "Category" + (sequenceNumber % 5 + 1);
            case "Production_Make_Buy_Code":
            case "Service_Make_Buy_Code":
                return random.nextBoolean() ? "Make" : "Buy";
            case "Module_End_Item":
            	return random.nextBoolean() ? "Yes" : "No";
            case "Responsible_Design_Engineer":
                return "Engineer" + (sequenceNumber % 5 + 1);
            case "Target_Cost":
                return 80 + random.nextDouble() * 720;
            case "Tooling_Cost":
                return 50 + random.nextDouble() * 450;
            case "Unit_of_Measure":
                return "EA";
            case "Originator":
                return "Originator" + (sequenceNumber % 5 + 1);
            case "Is_Version_Object":
                return random.nextBoolean();
            case "Move_Files_To_Version":
                return random.nextBoolean();
            case "Suspend_Versioning":
                return random.nextBoolean();
            case "Access_Type":
                return "Read-Only";
            case "Checkin_Reason":
                return "Initial Check-in";
            case "Language":
                return "English";
            case "File_Created_Date":
                return LocalDate.now().minusDays(random.nextInt(30));
            case "File_Dimension":
                return "1920x1080";
            case "File_Duration":
                return random.nextInt(300) + " seconds";
            case "File_Modified_Date":
                return LocalDate.now().minusDays(random.nextInt(10));
            case "File_Size":
                return (random.nextInt(500) + 1) + " KB";
            case "File_Type":
                return "PDF";
            case "Designated_User":
                return "User" + sequenceNumber;
            case "Title":
                return "Specification Title " + sequenceNumber;
            case "Model_Type":
                return "3D Model";
            case "IEF_LockInformation":
                return random.nextBoolean() ? "Locked" : "Unlocked";
            case "MCADInteg_SourceObj":
                return "SourceObject" + sequenceNumber;
            case "IEF_UUID":
                return "UUID-" + sequenceNumber;
            case "DSC_IsReplacementDone":
                return random.nextBoolean();
            case "IEF_Specification":
                return "Specification Document " + sequenceNumber;
            case "IEF_NewRevisedFrom":
                return "Rev" + (sequenceNumber - 1);
            case "MCADInteg_ClonedFrom":
                return "Clone-" + sequenceNumber;
            case "Renamed_From":
                return "OldName" + sequenceNumber;
            case "IEF_ExcludeFromBOM":
                return random.nextBoolean();
            case "IEF_ConnectedECO":
                return "ECO-" + sequenceNumber;
            case "CAD_Type":
                return "Mechanical CAD";
            case "MCAD_Label":
                return "Label-" + sequenceNumber;
            case "MCADInteg_Comment":
                return "Comment for Specification " + sequenceNumber;
            case "Source":
                return "Internal";
            case "IEF_EBOMSync_PartTypeAttribute":
                return "PartType" + sequenceNumber;
            case "FileName":
                return "File" + sequenceNumber + ".pdf";
            case "Active_Version_Ids":
                return "V1.0";
            case "Actual_Start_Date":
            case "Estimated_Start_Date":
            case "Absence_Start_Date":
            	return LocalDate.now();
            case "Responsible_Desin_Engineer":
            	return "rachalla";
            case "Reason_For_Cancel":
            	return "Cancel Reason for CA";
            case "Estimated_Completion_Date":
            case "Actual_Completion_Date":
            case "Actual_End_Date":
            case "Estimated_End_Date":
            case "Absence_End_Date":
            case "Last_Login_Date":
            	return LocalDate.now();
            case "Category_of_Change":
            	return "Platform Change";
            case "Severity":
            	return "High";
            case "Synopsys":
            	return "Synopsys for CA";
            case "Reason_for_Change":
            	return "Change Reason for CA";
            case "Reported_Against":
            	return generateYes();
            case "Action_Taken":
            	return random.nextBoolean() ? "Yes" : "-";
            case "Escalation_Required":
            	return random.nextBoolean() ? "Yes" : "No";
            case "Internal_Priority":
            	return random.nextBoolean() ? "Yes" : "No";
            case "Resolution_Recommendation":
            	return "Recommendation" + sequenceNumber;
            case "Resolution_Statement":
            	return "Resolution Statement" + sequenceNumber;
            case "Steps_To_Reproduce":
            	return"Steps to repoduce"+sequenceNumber;
            case "Priority":
            	return random.nextBoolean() ? "Priority" : "Non-Priority";
            case "Problem_Type":
            	return "Problem Type"+sequenceNumber;
            case "Resolution_Date":
            	return LocalDate.now().plusWeeks(1);
            case "ApprovedDate":
            	return random.nextBoolean() ? LocalDate.now().plusDays(5) : "-";
            case "ClosedDate":
            	return LocalDate.now().plusDays(4);
            case "CancelledDate":
            	return random.nextBoolean() ? LocalDate.now().plusDays(7) : "-";
            case "Address":
            	return "Address"+sequenceNumber;
            case "Email_Address":
            	return "person"+ sequenceNumber+"@gmail.com";
            case "Organization_Fax_Number":
            	return "(" + areaCode + ") " + middlePart + "-" + lastPart;
            case "First_Name":
            	return "Supplier"+ sequenceNumber;
            case "Last_Name":
            	return "Person"+ sequenceNumber;
            case "Middle_Name":
            	return "SPName"+ sequenceNumber;
            case "Web_Site":
            case "Website":
            	return "www.supplier"+ sequenceNumber+".com";
            case "Absence_Delegate":
            	return "Absence Delegate Person"+ (sequenceNumber % 5 + 1);
            case "Continent_Code":
            	return "Conti_Code" + (sequenceNumber % 5 + 1);
            case "Country_Code":
            	return String.format("CONTRY_CODE-", sequenceNumber);
            case "Country_Code_Short":
            	return "CONTY_CD"+sequenceNumber;
            case "Country_Code_Long":
            	return String.format("CONTRY_CODE-%07d", sequenceNumber);
            case "Status":
            	return random.nextBoolean() ? "Active" : "Inactive";
            default:
                return null;
        }
    }
    
    private static String getRandomValues(ArrayList<String> list, int maxCount) {
        if (list.isEmpty()) {
            return ""; // Return empty string if the list is empty
        }

        Random random = new Random();
        int count = random.nextInt(Math.min(maxCount, list.size())) + 1; // Generate between 1 and maxCount or the list size
        Collections.shuffle(list); // Shuffle the list to get random elements

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(list.get(i));
            if (i < count - 1) {
                result.append("|"); // Append the separator for all but the last value
            }
        }

        return result.toString();
    }
    
    private static String generateState() {
        List<String> states = Arrays.asList("Prepare", "In Work", "Frozen", "Submitted", "Released");
        stateCounter++;

        if (stateCounter <= MIN_RELEASED_STATES) {
            return "Released";
        } else {
            return states.get(new Random().nextInt(states.size()));
        }
    }
    
    private static String generateYes() {
    	List<String> reported = Arrays.asList("No","-");
    	yesCounter++;
    	
    	if (yesCounter < MIN_YES) {
            return "Yes";
        } else {
            return reported.get(new Random().nextInt(reported.size()));
        }
    }

    // Method to reset counter 
    public static void resetStateCounter() {
        stateCounter = 0;
        yesCounter = 0;
    }

    private static void createPartMPNRelationships(Connection connection, LinkedHashMap<String,String> imap) throws SQLException {
        String columns = (String) imap.get("mpn_related_parts_details");
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.mpn_related_parts_details";

            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                for (int i = 0; i < alPartIds.size(); i++) {
                    String partId = alPartIds.get(i);
                    String mpnId = alMPNIds.get(i);
                    int incrementedValue = idCounter.incrementAndGet();
                    for (int j = 0; j < columnArray.length; j++) {
                        String columnName = columnArray[j].trim();
                        	Object value = generateRelIdValue(columnName, partId, mpnId, incrementedValue,i+1);
                        	preparedStatement.setObject(j + 1, value);
                    }
                    
                    int rowsInserted = preparedStatement.executeUpdate();
                }
            }
        }
    }

    private static Object generateRelIdValue(String columnName, String sPartId, String sMPNId, int incrementedValue, int sequence) {
        switch (columnName) {
            case "PartId":
                return sPartId;
            case "MPNId":
                return sMPNId;
            case "RelId":
                return String.format("00001.00001.00001.%05d", incrementedValue);
            case "Type":
                return "Related Parts";
            case "Name":
                return String.format("%07d-00-A", sequence);
            case "Rev":
                return "01";
            default:
                return null;
        }
    }

    private static void createPartSpecificationRelationships(Connection connection, LinkedHashMap<String,String> imap) throws SQLException {
        String columns = (String)imap.get("specifications_related_objects.table.columns");
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.specifications_related_objects";

            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                Random random = new Random();
                Set<String> usedCombinations = new HashSet<>();

                for (String partId : alPartIds) {
                    int numSpecs = random.nextInt(4) + 2; // 2 to 5 specifications
                    List<String> selectedSpecs = new ArrayList<>(alSpecIds);
                    Collections.shuffle(selectedSpecs);
                    for (String specId : selectedSpecs) {
                        String combination = partId + "-" + specId;
                        if (!usedCombinations.contains(combination) && numSpecs > 0) {
                            usedCombinations.add(combination);
                            int incrementedValue = idCounter.incrementAndGet();

                            for (int j = 0; j < columnArray.length; j++) {
                                String columnName = columnArray[j].trim();
                                Object value = generateSpecRelationshipValue(columnName, partId, idNameMapPart.get(partId), specId, incrementedValue);
                                preparedStatement.setObject(j + 1, value);
                            }
                            
                            int rowsInserted = preparedStatement.executeUpdate();
                            
                            numSpecs--;
                        }

                        if (numSpecs == 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private static Object generateSpecRelationshipValue(String columnName, String partId, String partName, String specId, int incrementedValue) {
        switch (columnName) {
            case "SpecId":
                return specId;
            case "RelationshipName":
                return "Part Specification";
            case "RelatedObjId":
                return partId;
            case "RelatedObjType":
                return "Part";
            case "RelatedObjName":
                return partName;
            case "RelatedObjRev":
                return "01";
            case "RelId":
                return String.format("00001.00001.00001.%05d", incrementedValue);
            default:
                return null;
        }
    }


    private static void createChangeActionAffectedItems(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        String columns = propertiesMap.get("change_action_affected_items.table.columns");
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.change_action_affected_items";

            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                for (String changeActionNumber : changeActionPartDetailsMap.keySet()) {
                    String reasonForChange = "Reason for Change " + changeActionNumber;
                    changeActionReasons.put(changeActionNumber, reasonForChange);
                    ArrayList<ArrayList<String>> partDetailsList = changeActionPartDetailsMap.get(changeActionNumber); // Using List to maintain order
                    
                    for (ArrayList<String> eachPartDetails : partDetailsList) {;

                    	String partId = eachPartDetails.size() > 0 ? eachPartDetails.get(0) : "";
                        String partType = eachPartDetails.size() > 1 ? eachPartDetails.get(1) : "";
                        String partName = eachPartDetails.size() > 2 ? eachPartDetails.get(2) : "";
                        String version = eachPartDetails.size() > 3 ? eachPartDetails.get(3) : "";

                    		for (int j = 0; j < columnArray.length; j++) {
                    			String columnName = columnArray[j].trim();
                            
                    			Object value = generateChangeActionAffectedItemValue(columnName, changeActionNumber, 
                    					new String[]{partId, partType, partName, version}, reasonForChange);
                    			preparedStatement.setObject(j + 1, value);
                    		}

                    		int rowsInserted = preparedStatement.executeUpdate();
                    }
                }
            }
        }
    }


    private static Object generateChangeActionAffectedItemValue(String columnName, String changeActionNumber, String[] itemDetails, String reasonForChange) {
        Random random = new Random();
        switch (columnName) {
            case "Id":
                return itemDetails[0];
            case "Type":
                return itemDetails[1];
            case "Name":
                return itemDetails[2];
            case "Rev":
                return itemDetails[3];
            case "ChangeNumber":
                return changeActionNumber;
            case "RequestedChange":
                return "Requested Change for " + itemDetails[2];
            case "Reason_For_Change":
                return reasonForChange;
            case "Action":
                return random.nextBoolean() ? "Update" : "Replace";
            case "TrueEndItem":
                return random.nextBoolean();
            case "QuantityPerInstance":
                return random.nextInt(10) + 1;
            case "TorqueCriticalityImpacted":
                return random.nextBoolean();
            case "QtyToModify":
                return random.nextInt(100) + 1;
            case "IsSerializationImpacted":
                return random.nextBoolean();
            case "QtyInInventory":
                return random.nextInt(1000) + 1;
            case "QtyInField":
                return random.nextInt(10000) + 1;
            case "InstancePerVehicle":
                return random.nextInt(5) + 1;
            case "IsTorqueAffected":
                return random.nextBoolean();
            case "DispositionOnOrder":
                return "Disposition for On Order";
            case "DispositionInStock":
                return "Disposition for In Stock";
            case "DispositionInProcess":
                return "Disposition for In Process";
            case "DispositionInField":
                return "Disposition for In Field";
            case "DispositionFieldReturn":
                return "Disposition for Field Return";
            default:
                return null;
        }
    }
    
    private static void associateDeviationsWithPartDetails() {

        for (int i = 0; i < devIds.size() && i < partDetailsList.size(); i++) {
            String deviationId = devIds.get(i);
            ArrayList<String> partDetail = partDetailsList.get(i);
            String partId = partDetail.get(0);
            String partName = partDetail.get(2);
            String partDetailsString = String.join(",", partId, "Part", partName, "01");


            deviationPartMap.putIfAbsent(deviationId, new ArrayList<>());
            deviationPartMap.get(deviationId).add(partDetailsString);
        }
    }
    
    private static void createDeviationReportedAgainst(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        String columns = propertiesMap.get("deviation_Reported_Against.table.column");
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.deviation_reported_against_details";

            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                for (String devId : deviationPartMap.keySet()) {
                    List<String> partDetailsList = deviationPartMap.get(devId); // Using List to maintain order
                    
                    for (String partDetails : partDetailsList) {
                        // Split part details ensuring that the order is as expected
                        String[] itemDetails = partDetails.split(",");

                        // Explicitly map each item based on known positions to maintain correct associations
                        String partId = itemDetails.length > 0 ? itemDetails[0] : "";
                        String partType = itemDetails.length > 1 ? itemDetails[1] : "";
                        String partName = itemDetails.length > 2 ? itemDetails[2] : "";
                        String version = itemDetails.length > 3 ? itemDetails[3] : "";

                        for (int j = 0; j < columnArray.length; j++) {
                            String columnName = columnArray[j].trim();
                            // Ensure the correct values are associated with each column
                            Object value = generateDeviationReportedAgainstValue(columnName, devId, 
                                new String[]{partId, partType, partName, version});
                            preparedStatement.setObject(j + 1, value);
                        }

                        int rowsInserted = preparedStatement.executeUpdate();
                    }
                }
            }
        }
    }
    
    private static Object generateDeviationReportedAgainstValue(String columnName, String devId, String[] itemDetails) {
        Random random = new Random();
        switch (columnName) {
        	case "DeviationId":
        		return devId;
            case "Id":
                return itemDetails[0];
            case "Type":
                return itemDetails[1];
            case "Name":
                return itemDetails[2];
            case "Rev":
                return itemDetails[3];
            case "DispositionOnOrder":
                return "Disposition for On Order";
            case "DispositionInStock":
                return "Disposition for In Stock";
            case "DispositionInField":
                return "Disposition for In Field";
            default:
                return null;
        }
    }
    
    public static void createEBOMConnections(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
    	Random random = new Random();
        String columns = propertiesMap.get("ebom_details.table.columns");
     if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.ebom_details";

            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            Set<String> usedCombinations = new HashSet<>();

            // Create some parts without parents (top-level assemblies)
            int topLevelCount = random.nextInt(5) + 3; // 3 to 7 top-level assemblies
            List<String> topLevelParts = new ArrayList<>(alPartIds.subList(0, topLevelCount));

            for (String parentId : alPartIds) {
                if (topLevelParts.contains(parentId)) {
                    // This is a top-level part, it doesn't need a parent
                    continue;
                }

                int numChildren = random.nextInt(5) + 1; // 1 to 5 children
                List<String> availableChildren = new ArrayList<>(alPartIds);
                availableChildren.remove(parentId); // Remove self to prevent self-referencing
                Collections.shuffle(availableChildren);

                for (String childId : availableChildren) {
                    String combination = parentId + "-" + childId;
                    if (!usedCombinations.contains(combination) && numChildren > 0) {
                        usedCombinations.add(combination);
                        int incrementedValue = idCounter.incrementAndGet();

                        for (int j = 0; j < columnArray.length; j++) {
                            String columnName = columnArray[j].trim();
                            Object value = generateEBOMValue(columnName, parentId, childId, incrementedValue);
                            preparedStatement.setObject(j + 1, value);
                        }

                        int rowsInserted = preparedStatement.executeUpdate();

                        numChildren--;
                    }

                    if (numChildren == 0) {
                        break;
                    }
                }
            }
        }
    }
    }

    private static Object generateEBOMValue(String columnName, String parentId, String childId, int incrementedValue) {
    	Random random = new Random();
        switch (columnName) {
            case "EBOMRelId":
            	return String.format("00001.00001.00001.%05d", incrementedValue);
            case "Originated":
            case "Modified":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            case "Project":
                return "Default_Project";
            case "Organization":
                return "Default_Organization";
            case "Owner":
                return "Default_Owner";
            case "FromType":
            case "ToType":
                return "Part";
            case "FromName":
                return idNameMapPart.get(parentId);
            case "ToName":
                return idNameMapPart.get(childId);
            case "FromRev":
            case "ToRev":
                return "A";
            case "FromId":
                return parentId;
            case "ToId":
                return childId;
            case "V_Description":
                return "EBOM Relationship";
            case "IsVPMVisible":
                return true;
            case "Quantity":
                return random.nextInt(10) + 1;
            case "Find_Number":
                return String.format("FN%05d", random.nextInt(100000));
            case "Unit_of_Measure":
                return "EA";
            case "Start_Effectivity_Date":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "End_Effectivity_Date":
                return LocalDateTime.now().plusYears(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            default:
                return null;
        }
    }
    
    private static void associatePersonDetailsWithCompanyDetails(Map<String, Object> personRow) {
		String personId = (String) personRow.get("PersonId");
        String personName = (String) personRow.get("Name");
		String companyId = (String) personRow.get("CompanyId");
        String companyName = companyIdNameMap.get(companyId);
		String perComDetailsString = String.join(",", personName, companyId,companyName);
		personCompanyMap.put(personId, new ArrayList<>());
        personCompanyMap.get(personId).add(perComDetailsString);
    }
    
    private static void createSupplierPersonMemberCompany(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        String columns = propertiesMap.get("supplier_person_member_company_details.table.columns");
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.supplier_person_member_company_details";

            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                for (String personId : personCompanyMap.keySet()) {
                    List<String> personDetailsList = personCompanyMap.get(personId); // Using List to maintain order
                    
                    for (String personDetails : personDetailsList) {
                        String[] itemDetails = personDetails.split(",");

                        String personName = itemDetails.length > 0 ? itemDetails[0] : "";
                        String companyId = itemDetails.length > 1 ? itemDetails[1] : "";
                        String companyName = itemDetails.length > 2 ? itemDetails[2] : "";

                        for (int j = 0; j < columnArray.length; j++) {
                            String columnName = columnArray[j].trim();
                            Object value = generateSupplierPersonMemberCompanyValue(columnName, personId, 
                                new String[]{personName, companyId, companyName});
                            preparedStatement.setObject(j + 1, value);
                            
                            supplierRow.put(columnName, value);
                            
                        }
                        processSupplierRow(supplierRow);

                        int rowsInserted = preparedStatement.executeUpdate();
                    }
                }
            }
        }
    }
    
    private static void processSupplierRow(Map<String, Object> supplierRow) {
        String supId = (String) supplierRow.get("CompanyId");
        String supName = (String) supplierRow.get("CompanyName");
        
        ArrayList<String> supDetails = new ArrayList<>();
        supDetails.add(supId);
        supDetails.add(supName);
        
        if (supMap.containsKey(supDetails)) {
            supMap.put(supDetails, supMap.get(supDetails) + 1);
        } else {
            supMap.put(supDetails, 1);
        }
        
        // Clear the previous supCountList
        supCountList.clear();
        
        // Convert the map to the required output format
        for (Map.Entry<ArrayList<String>, Integer> entry : supMap.entrySet()) {
            ArrayList<String> newList = new ArrayList<>(entry.getKey());
            newList.add(String.valueOf(entry.getValue())); // Add the count at the end
            supCountList.add(newList);
        }
    }
	
	private static Object generateSupplierPersonMemberCompanyValue(String columnName, String personId, String[] itemDetails) {
        Random random = new Random();
        switch (columnName) {
        	case "PersonId":
        		return personId;
            case "PersonName":
                return itemDetails[0];
            case "CompanyId":
                return itemDetails[1];
            case "CompanyName":
                return itemDetails[2];           
            default:
                return null;
        }
    }
	
	public static void createAlternatePartDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        Random random = new Random();
        String columns = propertiesMap.get("alternate_part_details.table.columns");
        
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.alternate_part_details";
            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                Set<String> usedCombinations = new HashSet<>();
                Set<String> processedIds = new HashSet<>();
                
                for (String id : alPartIds) {
                    if (processedIds.contains(id)) {
                        continue;
                    }
                    
                    if (random.nextBoolean()) {// Randomly decide if this part should have an alternate
                        List<String> availableAlternates = new ArrayList<>(alPartIds);
                        availableAlternates.removeAll(processedIds); // Remove all processed IDs
                        availableAlternates.remove(id); // Remove self to prevent self-referencing
                        
                        if (!availableAlternates.isEmpty()) {
                            Collections.shuffle(availableAlternates);
                            String alternateId = availableAlternates.get(0);
                            
                            String combination = id + "-" + alternateId;
                            String reverseCombination = alternateId + "-" + id;
                            
                            if (!usedCombinations.contains(combination) && !usedCombinations.contains(reverseCombination)) {
                                usedCombinations.add(combination);
                                usedCombinations.add(reverseCombination); // Prevent reverse combination
                                
                                insertAlternatePart(preparedStatement, columnArray, id, alternateId);
                                
                                processedIds.add(id);
                                processedIds.add(alternateId);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void insertAlternatePart(PreparedStatement preparedStatement, String[] columnArray, String id, String alternateId) throws SQLException {
        for (int j = 0; j < columnArray.length; j++) {
            String columnName = columnArray[j].trim();
            Object value = generateAlternatePartValue(columnName, id, alternateId);
            preparedStatement.setObject(j + 1, value);
        }
        
        int rowsInserted = preparedStatement.executeUpdate();
    }

    private static Object generateAlternatePartValue(String columnName, String id, String alternateId) {
        switch (columnName) {
            case "Type":
                return "Part";
            case "Name":
                return idNameMapPart.get(id);
            case "Rev":
                return "01";
            case "Id":
                return id;
            case "Alternate_Id":
                return alternateId;
            default:
                return null;
        }
    }
    
    public static void createLastProcessedDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        String columns = propertiesMap.get("last_processed_details.table.columns");

        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.last_processed_details";
            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                for (int j = 0; j < columnArray.length; j++) {
                    String columnName = columnArray[j].trim();
                    Object value = generateLastProcessedDetails(columnName);
                    preparedStatement.setObject(j + 1, value);
                }
                
                int rowsInserted = preparedStatement.executeUpdate();
            }
        }
    }

    private static Object generateLastProcessedDetails(String columnName) {
        switch (columnName) {
            case "Last_Processed_Date":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            default:
                return null;
        }
    }
    
    private static void createCaDeviationSuppliersDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        String columns = propertiesMap.get("ca_deviation_suppliers_details.table.columns");
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.ca_deviation_suppliers_details";
            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );
            
            int incrementedValue = idCounter.incrementAndGet();
            Random random = new Random();
            
            String[] companyIds = companyIdNameMap.keySet().toArray(new String[0]);
            
            
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            	for (ArrayList<String> caDetails : caDetailsList) {
                    String changeNumber = caDetails.size() > 0 ? caDetails.get(0) : "";
                    String changeType = caDetails.size() > 1 ? caDetails.get(1) : "";
                    String changeName = caDetails.size() > 2 ? caDetails.get(2) : "";
                    String changeRev = caDetails.size() > 3 ? caDetails.get(3) : "";
                    String randomCompanyId = companyIds[random.nextInt(companyIds.length)];
                    String randomCompanyName = companyIdNameMap.get(randomCompanyId);
                    
                    for (int j = 0; j < columnArray.length; j++) {
                    	
                        String columnName = columnArray[j].trim();
                        Object value = generateCaDeviationSuppliersDetailsValue(columnName,
                            new String[]{changeNumber, changeType, changeName, changeRev}, incrementedValue, new String[]{randomCompanyId, randomCompanyName},changeNumber);
                        preparedStatement.setObject(j + 1, value);
                    }
                    int rowsInserted = preparedStatement.executeUpdate();
                }
                
                for (ArrayList<String> devDetails : devDetailsList) {
                    String devNumber = devDetails.size() > 0 ? devDetails.get(0) : "";
                    String devType = devDetails.size() > 1 ? devDetails.get(1) : "";
                    String devName = devDetails.size() > 2 ? devDetails.get(2) : "";
                    String devRev = devDetails.size() > 3 ? devDetails.get(3) : "";
                    String randomCompanyId = companyIds[random.nextInt(companyIds.length)];
                    String randomCompanyName = companyIdNameMap.get(randomCompanyId);
                    for (int j = 0; j < columnArray.length; j++) {
                        String columnName = columnArray[j].trim();
                        Object value = generateCaDeviationSuppliersDetailsValue(columnName, 
                            new String[]{devNumber, devType, devName, devRev}, incrementedValue, new String[]{randomCompanyId, randomCompanyName},devName);
                        preparedStatement.setObject(j + 1, value);
                    }
                    int rowsInserted = preparedStatement.executeUpdate();
                }   
            }
        }
    }

    private static Object generateCaDeviationSuppliersDetailsValue(String columnName, String[] caDevItemDetails, int incrementedValue, String[] companyDetails, String changeActionNumber) {
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime randomDate = now.minusDays(random.nextInt(365));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // Get part details for the given CA number
        ArrayList<ArrayList<String>> partDetailsList = changeActionPartDetailsMap.get(changeActionNumber);

        switch (columnName) {
            case "type":
                return caDevItemDetails[1];
            case "Name":
                return caDevItemDetails[2];
            case "Rev":
                return caDevItemDetails[3];
            case "ChangeNumber":
                return caDevItemDetails[0];
            case "CompanyId":
                return companyDetails[0];
            case "Company_Name":
                return companyDetails[1];
            case "Supplier_Visibility":
                String[] visibilityOptions = {"Owned Parts", "", "All"};
                return visibilityOptions[random.nextInt(visibilityOptions.length)];
            case "Supplier_Item_Visibility":
                // If partDetailsList is null or empty, return "-" randomly some times
                if (partDetailsList == null || partDetailsList.isEmpty()) {
                    return "";
                }
                
                // Randomly return "-" or the concatenated part IDs
                String partIds = partDetailsList.stream()
                                                .map(details -> details.get(0))
                                                .collect(Collectors.joining("|"));
                
                return random.nextBoolean() ? partIds : "";

            case "Supplier_Spec_Visibility":
                String itemVisibility = (String) generateCaDeviationSuppliersDetailsValue("Supplier_Item_Visibility", caDevItemDetails, incrementedValue, companyDetails, changeActionNumber);

                if ("-".equals(itemVisibility)) {
                    if (partDetailsList != null && !partDetailsList.isEmpty()) {
                        return partDetailsList.get(random.nextInt(partDetailsList.size())).get(0);
                    }
                    return "";
                } else {
                    String[] itemParts = itemVisibility.split("\\|");
                    if (itemParts.length == 1) {
                        return random.nextBoolean() ? itemParts[0] : "";
                    } else {
                        int numberOfParts = random.nextInt(itemParts.length) + 1; // Get at least one part
                        List<String> randomPartIds = Arrays.stream(itemParts)
                                                           .limit(numberOfParts)
                                                           .collect(Collectors.toList());
                        return String.join("|", randomPartIds);
                    }
                }
            case "Acknowledge":
                return random.nextBoolean() ? "Yes" : "No";
            case "AcknowledgedBy":
                return "User" + random.nextInt(1000);
            case "ProcessedBy":
                return "Processor" + random.nextInt(100);
            case "ProcessedDate":
                return randomDate.format(formatter);
            case "ProcessInfo":
                return "Process_" + random.nextInt(10);
            case "ProcessingComment":
                return "Comment_" + random.nextInt(100);
            case "relid":
                return String.format("00001.00001.00001.%05d", incrementedValue);
            default:
                return null;
        }
    }


    
    private static void createPartLastReleasedDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        String columns = propertiesMap.get("ec_parts_last_released.table.columns");
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.ec_parts_last_released";
            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            	for (ArrayList<String> partDetails : partDetailsList) {
                    String partId = partDetails.size() > 0 ? partDetails.get(0) : "";
                    String partType = partDetails.size() > 1 ? partDetails.get(1) : "";
                    String partName = partDetails.size() > 2 ? partDetails.get(2) : "";
                    String partRev = partDetails.size() > 3 ? partDetails.get(3) : "";
                    for (int j = 0; j < columnArray.length; j++) {
                        String columnName = columnArray[j].trim();
                        Object value = generatePartLastReleasedDetailsValue(columnName,
                            new String[]{partId, partType, partName, partRev});
                        preparedStatement.setObject(j + 1, value);
                    }
                    int rowsInserted = preparedStatement.executeUpdate();
                }  
            }
        }
    }

    private static Object generatePartLastReleasedDetailsValue(String columnName, String[] partDetails) {
        switch (columnName) {
            case "type":
                return "Part";
            case "Name":
                return partDetails[2];
            case "Rev":
                return partDetails[3];
            case "Id":
                return partDetails[0];
            default:
                return null;
        }
    }
    
    private static void createSupplierCountsDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        String columns = propertiesMap.get("supplier_counts.table.columns");
        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.supplier_counts";
            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            	for (ArrayList<String> supDetails : supCountList) {
                    String comId = supDetails.size() > 0 ? supDetails.get(0) : "";
                    String compName = supDetails.size() > 1 ? supDetails.get(1) : "";
                    String count = supDetails.size() > 2 ? supDetails.get(2) : "";
                    for (int j = 0; j < columnArray.length; j++) {
                        String columnName = columnArray[j].trim();
                        Object value = generateSupplierCountsDetailsValue(columnName,
                            new String[]{comId, compName, count});
                        preparedStatement.setObject(j + 1, value);
                    }
                    int rowsInserted = preparedStatement.executeUpdate();
                }  
            }
        }
    }

    private static Object generateSupplierCountsDetailsValue(String columnName, String[] supDetails) {
        switch (columnName) {
            case "Type":
                return "Company";
            case "CompanyName":                                                                                                                                                                                                                                                                                                                                                                                                        
                return supDetails[1];
            case "CompanyId":
                return supDetails[0];
			case "Total":
                return supDetails[2];
            default:
                return null;
        }
    }
    
	public static void createLoginDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
        String columns = propertiesMap.get("login_details.table.columns");

        if (columns != null && !columns.isEmpty()) {
            String[] columnArray = columns.split(",");
            String tableName = "supplierportal_schema1.login_details";
            String placeholders = generatePlaceholders(columnArray.length);
            String insertSQL = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                String.join(",", columnArray),
                placeholders
            );

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                for (int j = 0; j < columnArray.length; j++) {
                    String columnName = columnArray[j].trim();
                    Object value = generateLoginDetails(columnName);
                    preparedStatement.setObject(j + 1, value);
                    supplierInfo.put(columnName, value);
                }
                
                int rowsInserted = preparedStatement.executeUpdate();
            }
        }
    }

    private static Object generateLoginDetails(String columnName) {
	String email = "supplierportaladmin@gmail.com";
	String pass = "Admin@123";
	String bcryptHashed = BCrypt.hashpw(pass, BCrypt.gensalt());
        switch (columnName) {
            case "Email":
                return email;
			case "Password":
				return bcryptHashed;
			case "Username":
				return "supplierportaladmin";
			case "Firstname":
				return "supplierportal";
			case "Lastname":
				return "portaladmin";
			case "ConfirmPassword":
				return bcryptHashed;
			case "Country":
				return "India";
			case "Description":
				return "default description";
            default:
                return null;
        }
    }
    
	public static void createSupplierPersonDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
	    String columns = propertiesMap.get("supplier_person_details.table.columns");
	    int incrementedValue = idCounter.incrementAndGet();
	    int maxRowsToInsert = 3;  // Limit to 3 rows

	    if (columns != null && !columns.isEmpty()) {
	        String[] columnArray = columns.split(",");
	        String tableName = "supplierportal_schema1.supplier_person_details";
	        String placeholders = generatePlaceholders(columnArray.length);
	        String insertSQL = String.format(
	            "INSERT INTO %s (%s) VALUES (%s);",
	            tableName,
	            String.join(",", columnArray),
	            placeholders
	        );

	        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
	            for (int i = 0; i < maxRowsToInsert; i++) {
	                for (int j = 0; j < columnArray.length; j++) {
	                    String columnName = columnArray[j].trim();
	                    Object value = generateSupplierPersonDetailsValue(columnName, incrementedValue, i + 1,alCompanyIds.get(i));
	                    if("PersonId".equalsIgnoreCase(columnName)) {
	                    	companyIdPersonIdMap.put(alCompanyIds.get(i), (String)value);
	                    } 
	                    if("Name".equalsIgnoreCase(columnName)) {
	                    	companyNamePersonNameMap.put(alCompanyNames.get(i), (String)value);
	                    } 
	                    preparedStatement.setObject(j + 1, value);
	                    supplierInfo.put(columnName, value); 
	                }
	                preparedStatement.addBatch();
	                associatePersonDetailsWithCompanyDetails(supplierInfo);
	            }
	            int[] rowsInserted = preparedStatement.executeBatch();
	        }
	    }
	}


	    private static Object generateSupplierPersonDetailsValue(String columnName, int incrementedValue, int sequenceNumber,String companyId) {
		String email = "supplierportaladmin@gmail.com";
		String pass = "Admin@123";
		String bcryptHashed = BCrypt.hashpw(pass, BCrypt.gensalt());
		Random random = new Random();
	    String randomCompanyId = "";
		int areaCode = 200 + random.nextInt(800);
	    int middlePart = 100 + random.nextInt(900);
	    int lastPart = 1000 + random.nextInt(9000);
		
	        switch (columnName) {
	            case "PersonId" :
					return String.format("00001.00001.00001.%05d", idCounter.incrementAndGet());
				case "Type" :
					return "Supplier-Person";
				case "Name" :
					return String.format("SUP-%07d", sequenceNumber);
				case "Rev" :
					return "~";
				case "Policy" :
					return "-";
				case "State" :
					return generateState();
				case "Organization" :
					return "Engineering";
				case "Project" :
					return "Project" + sequenceNumber;
				case "Owner" :
					return "User" + sequenceNumber;
				case "Originated" :
				case "Modified" :
					return LocalDate.now();
				case "CompanyId" :
	                return companyId;
				case "Description" :
					return "Description for supplier "+sequenceNumber;
				case "Country" :
					return "India";
				case "Email_Address" :
					return "person"+ sequenceNumber+"@gmail.com";
				case "Organization_Fax_Number":
	            	return "(" + areaCode + ") " + middlePart + "-" + lastPart;
				case "First_Name":
	            	return "Supplier"+ sequenceNumber;
	            case "Last_Name":
	            	return "Person"+ sequenceNumber;
	            case "Middle_Name":
	            	return "SPName"+ sequenceNumber;
				case "Originator":
	                return "Originator" + (sequenceNumber % 5 + 1);
				case "Web_Site" :
					return "www.supplier"+ sequenceNumber+".com";
				case "Absence_Delegate":
	            	return "Absence Delegate Person"+ (sequenceNumber % 5 + 1);
				case "Absence_Start_Date" :
					return LocalDate.now();
				case "Absence_End_Date" :
					return LocalDate.now();
	            default:
	                return null;
	        }
	    }

	    public static void createCompanyDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
	        String columns = propertiesMap.get("company_details.table.columns");
	        int incrementedValue = idCounter.incrementAndGet();
	        int maxRowsToInsert = 3;  // Limit to 3 rows

	        if (columns != null && !columns.isEmpty()) {
	            String[] columnArray = columns.split(",");
	            String tableName = "supplierportal_schema1.company_details";
	            String placeholders = generatePlaceholders(columnArray.length);
	            String insertSQL = String.format(
	                "INSERT INTO %s (%s) VALUES (%s);",
	                tableName,
	                String.join(",", columnArray),
	                placeholders
	            );

	            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
	                for (int i = 0; i < maxRowsToInsert; i++) {
	                    String companyId = null;
	                    String companyName = null;

	                    for (int j = 0; j < columnArray.length; j++) {
	                        String columnName = columnArray[j].trim();
	                        Object value = generateCompanyDetailsValue(columnName, incrementedValue, i + 1);
	                        preparedStatement.setObject(j + 1, value);

	                        // Capture companyId and companyName for storing
	                        if (columnName.equals("CompanyId")) {
	                            companyId = (String) value;
	                        }
	                        if (columnName.equals("Name")) {
	                            companyName = (String) value;
	                            companyNameList.add((String)value);
	                        }

	                        companyInfo.put(columnName, value);
	                    }

	                    // Add to batch
	                    preparedStatement.addBatch();

	                    // After inserting, store companyId and companyName in respective lists
	                    if (companyId != null && companyName != null) {
	                        alCompanyIds.add(companyId);
	                        alCompanyNames.add(companyName);
	                    }
	                }

	                // Execute the batch insert
	                int[] rowsInserted = preparedStatement.executeBatch();
	            }

	            // Populate the companyIdNameMap after insertion
	            for (int j = 0; j < alCompanyIds.size(); j++) {
	                if (alCompanyIds.size() > j && alCompanyNames.size() > j) {
	                    String sCompanyId = alCompanyIds.get(j);
	                    String sCompanyName = alCompanyNames.get(j);
	                    companyIdNameMap.put(sCompanyId, sCompanyName);
	                }
	            }
	        }
	    }

	    private static Object generateCompanyDetailsValue(String columnName, int incrementedValue, int sequenceNumber) {
	        Random random = new Random();
	        int areaCode = 200 + random.nextInt(800);
	        int middlePart = 100 + random.nextInt(900);
	        int lastPart = 1000 + random.nextInt(9000);

	        switch (columnName) {
	            case "CompanyId":
	                return String.format("00001.00001.00001.%05d", idCounter.incrementAndGet());
	            case "Type":
	                return "Company";
	            case "Name":
	                return String.format("COM-%07d", sequenceNumber);
	            case "Rev":
	                return "~";
	            case "Policy":
	                return "Company";
	            case "State":
	                return generateState();
	            case "Organization":
	                return "Engineering";
	            case "Project":
	                return "Project" + sequenceNumber;
	            case "Owner":
	                return "User" + sequenceNumber;
	            case "Originated":
	            case "Modified":
	                return LocalDate.now();
	            case "Address":
	                return "Address" + sequenceNumber;
	            case "Description":
	                return "Description for supplier " + sequenceNumber;
	            case "Country":
	                return "India";
	            case "Organization_Fax_Number":
	                return "(" + areaCode + ") " + middlePart + "-" + lastPart;
	            default:
	                return null;
	        }
	    }

		public static void createChangeActionDetails(Connection connection, LinkedHashMap<String, String> propertiesMap) throws SQLException {
	        String columns = propertiesMap.get("change_action_details.table.columns");
	        int incrementedValue = idCounter.incrementAndGet();

	        if (columns != null && !columns.isEmpty()) {
	            String[] columnArray = columns.split(",");
	            String tableName = "supplierportal_schema1.change_action_details";
	            String placeholders = generatePlaceholders(columnArray.length);
	            String insertSQL = String.format(
	                "INSERT INTO %s (%s) VALUES (%s);",
	                tableName,
	                String.join(",", columnArray),
	                placeholders
	            );

	            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
	                for (int i = 0; i < OBJECTS_COUNT; i++) {
	                    String caId = null;
	                    String caName = null;

	                    for (int j = 0; j < columnArray.length; j++) {
	                        String columnName = columnArray[j].trim();
	                        Object value = generateChangeActionDetailsValue(columnName, incrementedValue, i + 1);
	                        preparedStatement.setObject(j + 1, value);
	                        
	                        caRow.put(columnName, value);
	                        // Capture companyId and companyName for storing
	                        if (columnName.equals("ChangeNumber")) {
	                            caId = (String) value;
								caIdList.add((String)value);
	                        }
	                        if (columnName.equals("Name")) {
	                            caName = (String) value;
	                            caNameList.add((String)value);
	                        }
	                    }
	                    processCaRow(caRow);
	                    // Add to batch
	                    preparedStatement.addBatch();

	                    // After inserting, store companyId and companyName in respective lists
	                    if (caId != null && caName != null) {
	                        alCaIds.add(caId);
	                        alCaNames.add(caName);
	                    }
	                }

	                // Execute the batch insert
	                int[] rowsInserted = preparedStatement.executeBatch();
	            }
	        }
	    }
		
		private static Object generateChangeActionDetailsValue(String columnName, int incrementedValue, int sequenceNumber) {
	        Random random = new Random();

	        switch (columnName) {
			case "ChangeNumber":
				return String.format("00001.00001.00001.%05d", idCounter.incrementAndGet());
			case "Type":
				return "Change Action";
			case "Name":
				return String.format("CA-%07d", sequenceNumber);
			case "Rev":
				return "-";
			case "Policy":
				return "Change Action";
			case "State":
				return generateState();
			case "Organization":
				return "Engineering";
			case "Project":
				return "Project" + sequenceNumber;
			case "Owner":
				return "User" + sequenceNumber;
			case "Originated":
			case "Modified":
			case "CurrentStateAttainedDate":
				return LocalDate.now();
			case "Description":
				return "Description for " + sequenceNumber;
			case "Originator":
				return "Originator" + (sequenceNumber % 5 + 1);
			case "Estimated_Start_Date":
			case "Actual_Start_Date":
				return LocalDate.now();
			case "Part_Classification":
				return "Class" + (sequenceNumber % 3 + 1);
			case "Responsible_Desin_Engineer":
				return "rachalla";
			case "Tooling_Cost":
				return 50 + random.nextDouble() * 450;case "Reason_For_Cancel":
			case "Estimated_Completion_Date":
			case "Actual_Completion_Date":		
				return LocalDate.now();
			case "Category_of_Change":
				return "Platform Change";
			case "Severity":
				return "High";
			case "Synopsys":
				return "Synopsys for CA";
			case "Reason_for_Change":
				return "Change Reason for CA";
			default:
                return null;

	        }
	    }
}
