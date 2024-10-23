@echo off
SETLOCAL

set psql_user=postgres
set psql_password=Xploria
set Database_name=supplierportal
set schema_name=supplierportal_schema1
set server_host=localhost
set server_port=5432

set PGPASSWORD=%psql_password%

echo Creating Database %Database_name%
psql -h %server_host% -p %server_port% -U %psql_user% -c "CREATE DATABASE %Database_name%"

if %errorlevel% neq 0 (
    echo Failed to create Database %Database_name%
    exit %errorlevel%
)

echo Creating Schema
psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE SCHEMA %schema_name%"

echo Creating Tables
psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.EC_Parts_Details (Id VARCHAR(255) NOT NULL PRIMARY KEY,Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(8),Policy VARCHAR(255),State VARCHAR(255),Organization VARCHAR(255),Project  VARCHAR(255),Owner VARCHAR(255),Originated VARCHAR(255),Modified VARCHAR(255),CurrentStateAttainedDate VARCHAR(255),ReleaseDate VARCHAR(255),IsLastRevision VARCHAR(255),ChangeNumber VARCHAR(255),Description VARCHAR(255),Spare_Part VARCHAR(255),Part_Classification VARCHAR(255),Effectivity_Date VARCHAR(255),End_Effectivity VARCHAR(255),Estimated_Cost VARCHAR(255),Lead_Time VARCHAR(255),Material_Category VARCHAR(255),Production_Make_Buy_Code VARCHAR(255),Responsible_Design_Engineer VARCHAR(255),Service_Make_Buy_Code VARCHAR(255),Target_Cost VARCHAR(255),Tooling_Cost VARCHAR(255),Unit_of_Measure VARCHAR(255),Module_End_Item VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Deviation_Details (DeviationId VARCHAR(255) NOT NULL PRIMARY KEY,Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(4),Policy VARCHAR(255),State VARCHAR(255),Organization VARCHAR(255),Project VARCHAR(255),Owner VARCHAR(255),Originated VARCHAR(255),Modified VARCHAR(255),CurrentStateAttainedDate VARCHAR(255),Reported_Against VARCHAR(255),Description VARCHAR(255),Originator VARCHAR(255),Action_Taken VARCHAR(255),Escalation_Required VARCHAR(255),Actual_Start_Date VARCHAR(255),Estimated_Start_Date VARCHAR(255),Actual_End_Date VARCHAR(255),Estimated_End_Date VARCHAR(255),Responsible_Design_Engineer VARCHAR(255),Internal_Priority VARCHAR(255),Title VARCHAR(255),Resolution_Recommendation VARCHAR(255),Resolution_Statement VARCHAR(255),Steps_To_Reproduce VARCHAR(255),Priority VARCHAR(255),Problem_Type VARCHAR(255),Resolution_Date VARCHAR(255),ApprovedDate VARCHAR(255),ClosedDate VARCHAR(255),CancelledDate VARCHAR(255),MPNDetails VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Alternate_Part_Details (Id VARCHAR(255) NOT NULL PRIMARY KEY,Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(4),Alternate_Id VARCHAR(255),FOREIGN KEY (Id) REFERENCES %schema_name%.EC_Parts_Details(Id));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE  %schema_name%.Company_Details (CompanyId VARCHAR(255) NOT NULL PRIMARY KEY,Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(255),Policy VARCHAR(255),State VARCHAR(255),Organization VARCHAR(255),Project VARCHAR(255),Owner VARCHAR(255),Originated VARCHAR(255),Modified VARCHAR(255),Description VARCHAR(255),Address VARCHAR(255),Cage_Code VARCHAR(255),City VARCHAR(255),Country VARCHAR(255),DUNS_Number VARCHAR(255),Division VARCHAR(255),Organization_Fax_Number VARCHAR(255),Organization_ID VARCHAR(255),Organization_Name VARCHAR(255),Organization_Phone_Number VARCHAR(255),postal_Code VARCHAR(255),State_Region VARCHAR(255),Website VARCHAR(255),File_Store_Symbolic_name VARCHAR(255),FTP_Directory VARCHAR(255),FTP_Host VARCHAR(255),File_Site VARCHAR(255),Alternate_Name VARCHAR(255),Primary_Key VARCHAR(255),Default_Policy VARCHAR(255),Meeting_Site_ID VARCHAR(255),Meeting_Site_Name VARCHAR(255),Standard_Cost VARCHAR(255),Distinguished_Name VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE  %schema_name%.EC_Parts_Last_Released (Id VARCHAR(255) NOT NULL PRIMARY KEY REFERENCES %schema_name%.EC_Parts_Details (Id),Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(8));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE  %schema_name%.Library_Structure_Details (Library VARCHAR(255),Parent_Class VARCHAR(255),Child_Class VARCHAR(255),Child_Id VARCHAR(255),Classification_Path VARCHAR(255),Library_Count VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE  %schema_name%.MPN_Details (MPNId VARCHAR(255) NOT NULL PRIMARY KEY,Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(255),Policy VARCHAR(255),State VARCHAR(255),Organization VARCHAR(255),Project VARCHAR(255),Owner VARCHAR(255),Originated VARCHAR(255),Modified VARCHAR(255),CurrentStateAttainedDate VARCHAR(255),ReleaseDate VARCHAR(255),CompanyName VARCHAR(255),Description VARCHAR(255),Originator VARCHAR(255),Responsible_Design_Engineer VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE  %schema_name%.MPN_Related_Parts_Details (PartId VARCHAR(255) REFERENCES %schema_name%.EC_Parts_Details (Id),Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(8),MPNId VARCHAR(255) REFERENCES %schema_name%.MPN_Details (MPNId),RelId VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE  %schema_name%.Supplier_Counts (CompanyId VARCHAR(255) REFERENCES %schema_name%.Company_Details (CompanyId),Type VARCHAR(255),CompanyName VARCHAR(255),Total VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Specifications_Details (Id VARCHAR(255) NOT NULL PRIMARY KEY,Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(4),Policy VARCHAR(255),State VARCHAR(255),Organization VARCHAR(255),Project VARCHAR(255),Owner VARCHAR(255),Originated VARCHAR(255),Modified VARCHAR(255),CurrentStateAttainedDate VARCHAR(255),ReleaseDate VARCHAR(255),Originator VARCHAR(255),Is_Version_Object VARCHAR(255),Move_Files_To_Version VARCHAR(255),Description VARCHAR(255),Suspend_Versioning VARCHAR(255),Access_Type VARCHAR(255),Checkin_Reason VARCHAR(255),Language VARCHAR(255),File_Created_Date VARCHAR(255),File_Dimension VARCHAR(255),File_Duration VARCHAR(255),File_Modified_Date VARCHAR(255),File_Size VARCHAR(255),File_Type VARCHAR(255),Designated_User VARCHAR(255),Title VARCHAR(255),Model_Type VARCHAR(255),IEF_LockInformation VARCHAR(255),MCADInteg_SourceObj VARCHAR(255),IEF_UUID VARCHAR(255),DSC_IsReplacementDone VARCHAR(255),IEF_Specification VARCHAR(255),IEF_NewRevisedFrom VARCHAR(255),MCADInteg_ClonedFrom VARCHAR(255),Renamed_From VARCHAR(255),IEF_ExcludeFromBOM VARCHAR(255),IEF_ConnectedECO VARCHAR(255),CAD_Type VARCHAR(255),MCAD_Label VARCHAR(255),MCADInteg_Comment VARCHAR(255),Source VARCHAR(255),IEF_EBOMSync_PartTypeAttribute VARCHAR(255),FileName VARCHAR(255),Active_Version_Ids VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Specifications_Related_Objects (SpecId VARCHAR(255) REFERENCES %schema_name%.Specifications_Details (Id),RelationshipName VARCHAR(255),RelatedObjId VARCHAR(255),RelatedObjType VARCHAR(255),RelatedObjName VARCHAR(255),RelatedObjRev VARCHAR(255),RelId VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Change_Action_Details (ChangeNumber VARCHAR(255),Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(4),Policy VARCHAR(255),State VARCHAR(255),Organization VARCHAR(255),Project VARCHAR(255),Owner VARCHAR(255),Originated VARCHAR(255),Modified VARCHAR(255),CurrentStateAttainedDate VARCHAR(255),Description VARCHAR(255),Originator VARCHAR(255),Actual_Start_Date VARCHAR(255),Estimated_Start_Date VARCHAR(255),Part_Classification VARCHAR(255),Responsible_Desin_Engineer VARCHAR(255),Tooling_Cost VARCHAR(255),Reason_For_Cancel VARCHAR(255),Estimated_Completion_Date VARCHAR(255),Actual_Completion_Date VARCHAR(255),Category_of_Change VARCHAR(255),Severity VARCHAR(255),Synopsys VARCHAR(255),Reason_for_Change VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE  %schema_name%.Change_Action_Affected_Items (Id VARCHAR(255),Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(255),ChangeNumber VARCHAR(255),RequestedChange VARCHAR(255),Reason_for_Change VARCHAR(255),Action VARCHAR(255),TrueEndItem VARCHAR(255),QuantityPerInstance VARCHAR(255),TorqueCriticalityImpacted VARCHAR(255),QtyToModify VARCHAR(255),IsSerializationImpacted VARCHAR(255),QtyInInventory VARCHAR(255),QtyInField VARCHAR(255),InstancePerVehicle VARCHAR(255),IsTorqueAffected VARCHAR(255),DispositionOnOrder VARCHAR(255),DispositionInStock VARCHAR(255),DispositionInProcess VARCHAR(255),DispositionInField VARCHAR(255),DispositionFieldReturn VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Data_Def_Selectables (jsonTableName VARCHAR(255),jsonType VARCHAR(255),Selectable VARCHAR(255),Where_Clause VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.EBOM_Details (EBOMRelId VARCHAR(255) NOT NULL PRIMARY KEY,Originated VARCHAR(255),Modified VARCHAR(255),Project VARCHAR(255),Organization VARCHAR(255),Owner VARCHAR(255),FromType VARCHAR(255),FromName VARCHAR(255),FromRev VARCHAR(255),FromId VARCHAR(255) REFERENCES %schema_name%.EC_Parts_Details (Id),ToType VARCHAR(255),ToName VARCHAR(255),ToRev VARCHAR(255),ToId VARCHAR(255) REFERENCES %schema_name%.EC_Parts_Details (Id),V_Description VARCHAR(255),IsVPMVisible VARCHAR(255),Quantity VARCHAR(255),Find_Number VARCHAR(255),Unit_of_Measure VARCHAR(255),Start_Effectivity_Date VARCHAR(255),End_Effectivity_Date VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.PCN_Info (Name VARCHAR(255),PCN_Info VARCHAR(255),Issued_Date VARCHAR(255),Sync_End_Date VARCHAR(255),Sync_Start_Date VARCHAR(255),PCN_Number VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Supplier_Person_Details (PersonId VARCHAR(255) NOT NULL PRIMARY KEY,Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(4),Policy VARCHAR(255),State VARCHAR(255),Organization VARCHAR(255),Project VARCHAR(255),Owner VARCHAR(255),Originated VARCHAR(255),Modified VARCHAR(255),CompanyId VARCHAR(255),Description VARCHAR(255),Login_Type VARCHAR(255),Address VARCHAR(255),City VARCHAR(255),Country VARCHAR(255),Email_Address VARCHAR(255),Fax_Number VARCHAR(255),First_Name VARCHAR(255),Home_Phone_Number VARCHAR(255),Last_Name VARCHAR(255),Middle_Name VARCHAR(255),Originator VARCHAR(255),Pager_Number VARCHAR(255),Postal_Code VARCHAR(255),State_Region VARCHAR(255),Web_Site VARCHAR(255),Work_Phone_Number VARCHAR(255),Absence_Delegate VARCHAR(255),Absence_End_Date VARCHAR(255),Absence_Start_Date VARCHAR(255),File_Store_Symbolic_Name VARCHAR(255),Last_Login_Date VARCHAR(255),Licensed_Hours VARCHAR(255),Mail_Code VARCHAR(255),Host_Meetings VARCHAR(255),Meeting_Key VARCHAR(255),Meeting_Password VARCHAR(255),Meeting_UserName VARCHAR(255),Subscription_Level VARCHAR(255),Cell_Phone_Number VARCHAR(255),Icon_Mail VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Supplier_Person_Member_Company_Details (PersonId VARCHAR(255) REFERENCES %schema_name%.Supplier_Person_Details (PersonId),CompanyId VARCHAR(255) REFERENCES %schema_name%.Company_Details (CompanyId),PersonName VARCHAR(255),CompanyName VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Roll_Label_Mapping (Roll VARCHAR(255),CS VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.CA_Deviation_Suppliers_Details (type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(8),ChangeNumber VARCHAR(255) ,CompanyId VARCHAR(255) REFERENCES %schema_name%.Company_Details (CompanyId),Company_Name VARCHAR(255),Supplier_Visibility VARCHAR(255),Supplier_Item_Visibility VARCHAR(255),Supplier_Spec_Visibility VARCHAR(255),Acknowledge VARCHAR(255),AcknowledgedBy VARCHAR(255),ProcessedBy VARCHAR(255),ProcessedDate VARCHAR(255),ProcessInfo VARCHAR(255),ProcessingComment VARCHAR(255),relid VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Country_Details (Name VARCHAR(255),Continent_Code VARCHAR(255),Country_Code VARCHAR(255),Country_Code_Short VARCHAR(255),Country_Code_Long VARCHAR(255),CountryId VARCHAR(255),Status VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE  %schema_name%.AttributeName_Label_Mapping (Type VARCHAR(255),Name VARCHAR(255),Description VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Deviation_Reported_Against_Details (DeviationId VARCHAR(255) REFERENCES %schema_name%.Deviation_Details (DeviationId),Type VARCHAR(255),Name VARCHAR(255),Rev VARCHAR(8),Id VARCHAR(255),DispositionInField VARCHAR(255),DispositionOnOrder VARCHAR(255),DispositionInStock VARCHAR(255));"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Last_Processed_Details (Last_Processed_Date VARCHAR(255))"

psql -h %server_host% -p %server_port% -U %psql_user% -d %Database_name% -c "CREATE TABLE %schema_name%.Login_Details (Email VARCHAR(255),Password VARCHAR(255),Username VARCHAR(255),Firstname VARCHAR(255),Lastname VARCHAR(255),ConfirmPassword VARCHAR(255),Country VARCHAR(255),Description VARCHAR(255),JWT VARCHAR(255),Preferences JSON);"

echo Successfully Created

:: Clean up
ENDLOCAL
pause
