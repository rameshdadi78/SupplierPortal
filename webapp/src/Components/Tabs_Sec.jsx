import "../Styles/Tabs_Sec.css";
import { Tabs, Tab, TabList, TabPanel } from 'react-tabs';
import { useState, useContext,useRef,useEffect } from 'react';
import { useCallback } from "react";
import { MainContext } from "./MainSection";
import { Tab_PartDetails } from "./Tab_PartDetails";
import { Tab_Specification } from "./Tab_Specification";
import { Tab_EBOM1 } from "./Tab_EBOM1";
import { Tab_Changes } from "./Tab_Changes";
import { Tab_Deviation } from "./Tab_Deviation";

export const Tabs_Sec = ({selectedRow,visibility,specVisibility,itemVisibility}) => {
  const [selectedTab, setSelectedTab] = useState(0); // Add useState to manage selected tab

  const [isSlideInOpen, setIsSlideInOpen] = useState(false);
  const [columnsData, setColumnsData] = useState([]);

  const [selectedRowData, setSelectedRowData] = useState(null);

  const tabs = [
    { name: "Part Detail(s)", icon: "/assets/part.png" },
    { name: "Specification(s)", icon: "/assets/specification.png" },
    { name: "EBOM", icon: "./assets/ebom.png" },
    { name: "Changes", icon: "/assets/ChangeAction.png" },
    { name: "Deviation(s)", icon: "/assets/Deviation.png" },
    { name: "Attachment(s)", icon: "/assets/Attachments_Icon.png" },
    { name: "Supplier Document(s)", icon: "/assets/SupplierPortal.png" },
  ];
// ----------------------


const [dropdownVisible, setDropdownVisible] = useState(false);
// ----------------------Export Common----------------


const exportButtonRef = useRef(); 

useEffect(() => {
  const handleClickOutside = (event) => {
    // Check if the click is outside both the dropdown and the export button
    if (
      exportDropdownRef.current && 
      !exportDropdownRef.current.contains(event.target) &&
      exportButtonRef.current && 
      !exportButtonRef.current.contains(event.target)
    ) {
      setExportDropdownVisible(false);
      setDropdownVisible(false);
    }
  };

  document.addEventListener('mousedown', handleClickOutside);
  return () => {
    document.removeEventListener('mousedown', handleClickOutside);
  };
}, []);




const [exportOption, setExportOption] = useState(null);
const [dataToExport, setDataToExport] = useState({ data: [], headers: [] });
const [exportDropdownVisible, setExportDropdownVisible] = useState(false);
const [currentExportType, setCurrentExportType] = useState(null); // To track whether CA or Spec export
const downloadRef = useRef(); // Ref for triggering download
const exportDropdownRef = useRef(); // Ref for the export dropdown

// --------Deviation
const [devAailableAttributes, setdevAailableAttributes] = useState([
  'Name', 'Title', 'Description', 'State', 'View Details' // Default attributes
]);
const [devVisibleColumns, setdevVisibleColumns] = useState(devAailableAttributes);
const [DevTableData, setDevTableData] = useState([]);

// -------Ca
const [availableAttributes, setAvailableAttributes] = useState([
  'Name',"Policy","Owner", 'State', 'Rev','File name', 'Synopsis', 'Description','View Affected Items' // Default attributes
]);
const [caData,setCaData]= useState([]);
const [visibleColumns, setVisibleColumns] = useState(availableAttributes);

// ------Specification
const [specData,setSpecData]= useState([]);

// -------Ebom
const [ebomAvailableAttributes, setEBOMAvailableAttributes] = useState([
  'Name', 'Description', 'Owner', // Default attributes
]);
const [ebomVisibleColumns, setebomVisibleColumns] = useState(ebomAvailableAttributes);
const [tableData, setTableData] = useState([]);
const isSpecVisible = specVisibility[0]  === "true";
  const isItemVisible = itemVisibility[0]  === "true";

      //Devation export
      const prepareExportData = useCallback((includeAllColumns) => {
        const columnsToInclude = includeAllColumns ? columnsData : columnsData.filter(col => devVisibleColumns.includes(col.name));
        const data = DevTableData.map(row => {
          const processedRow = {};
          columnsToInclude.forEach(col => {
            const key = col.name;
            processedRow[key] = Array.isArray(row[key]) ? row[key].join(', ') : row[key] || 'N/A';
          });
          return processedRow;
        });
      
        const headers = columnsToInclude
          .filter(col => col.name !== 'View Details') // Exclude 'View Details' from export
          .map(col => ({ label: col.name, key: col.name }));
      
        return { data, headers };
      }, [columnsData, devVisibleColumns, DevTableData]);

// Function to prepare CA export data
const prepareCAExportData = useCallback((includeAllColumns) => {
  const columnsToInclude = includeAllColumns ? columnsData : columnsData.filter(col => visibleColumns.includes(col.name));
  const data = caData.map(row => {
    const processedRow = {};
    columnsToInclude.forEach(col => {
      const key = col.name;
      processedRow[key] = Array.isArray(row[key]) ? row[key].join(', ') : row[key] || 'N/A';
    });
    return processedRow;
  });

  const headers = columnsToInclude
    .filter(col => col.name !== 'View Affected Items') // Exclude 'View Details' from export
    .map(col => ({ label: col.name, key: col.name }));

  return { data, headers };
}, [columnsData, visibleColumns, caData]);

// Function to prepare Specification export data
const prepareSpecExportData = useCallback((includeAllColumns) => {
  const columnsToInclude = includeAllColumns ? columnsData : columnsData.filter(col => visibleColumns.includes(col.name));
  const data = specData.map(row => {
    const processedRow = {};
    columnsToInclude.forEach(col => {
      const key = col.name;
      processedRow[key] = Array.isArray(row[key]) ? row[key].join(', ') : row[key] || 'N/A';
    });
    return processedRow;
  });

  const headers = columnsToInclude
    .filter(col => col.name !== 'View Affected Items') // Exclude 'View Details' from export
    .map(col => ({ label: col.name, key: col.name }));

  return { data, headers };
}, [columnsData, visibleColumns, specData]);
//EBOM
const prepareEBOMExportData = useCallback((includeAllColumns) => {
  const columnsToInclude = includeAllColumns ? columnsData : columnsData.filter(col => ebomVisibleColumns.includes(col.name));
  const data = tableData.map(row => {
    const processedRow = {};
    columnsToInclude.forEach(col => {
      const key = col.name;
      processedRow[key] = Array.isArray(row[key]) ? row[key].join(', ') : row[key] || 'N/A';
    });
    return processedRow;
  });

  const headers = columnsToInclude
    .filter(col => col.name !== 'View Details') // Exclude 'View Details' from export
    .map(col => ({ label: col.name, key: col.name }));

  return { data, headers };
}, [columnsData, ebomVisibleColumns, tableData])

const handleExportOptionChange = useCallback((option, exportType) => {
  const exportData = exportType === 'CA' ? prepareCAExportData(option) : exportType === 'Spec'
  ? prepareSpecExportData(option)
  : exportType === 'Deviation'
  ? prepareExportData(option)
  : prepareEBOMExportData(option);

  setDataToExport(exportData);
  setExportOption(option);
  setCurrentExportType(exportType); 
  setExportDropdownVisible(false); 
},
[prepareCAExportData, prepareSpecExportData,prepareEBOMExportData, prepareExportData]);

  const triggerDownload = () => {

    if (downloadRef.current) {
      downloadRef.current.link.click();
    }
  };
  useEffect(() => {
    if (exportOption !== null) {
      triggerDownload();
    }
  }, [exportOption, dataToExport]);
  
  const csvLinkKey = `${currentExportType}-${exportOption}-${dataToExport.data.length}`;
  const getVisibleTabs = () => {
    if (visibility) return tabs;
    if (isItemVisible) return [tabs[0]]; 
    if (isSpecVisible) return [tabs[0], tabs[1]]; 
    return [tabs[0]]; 
  };
  // --------------------------------------------



  return (

    <Tabs selectedIndex={selectedTab} onSelect={index => setSelectedTab(index)}>
       <TabList className="tab-list">
       {getVisibleTabs().map((tab, index) => (
          <Tab
            key={index}
            className={`tab-item ${selectedTab === index ? 'selectedTab' : 'unselectedTab'}`}
          >
            <div className="tab-content">
              <img src={tab.icon} className="tab-icon" alt={tab.name} />
              <div className="tab-name">{tab.name}</div>
            </div>
          </Tab>
        ))}
      </TabList>
{/* ------------------------------------------------------------------------------------------------------------ */}

      <TabPanel>
        <Tab_PartDetails selectedRow={selectedRow}/>
      </TabPanel>
      {isSpecVisible && (
        <>
      <TabPanel>
        <Tab_Specification handleExportOptionChange={handleExportOptionChange} exportDropdownVisible={exportDropdownVisible} setExportDropdownVisible={setExportDropdownVisible} currentExportType={currentExportType} isSlideInOpen={isSlideInOpen} setIsSlideInOpen={setIsSlideInOpen} columnsData={columnsData} setColumnsData={setColumnsData} selectedRowData={selectedRowData} setSelectedRowData={setSelectedRowData} downloadRef={downloadRef} exportDropdownRef={exportDropdownRef} exportButtonRef={exportButtonRef} availableAttributes={availableAttributes} setAvailableAttributes={setAvailableAttributes} exportOption={exportOption} dataToExport={dataToExport} csvLinkKey={csvLinkKey} specData={specData} setSpecData={setSpecData} visibleColumns={visibleColumns} setVisibleColumns={setVisibleColumns} selectedRow={selectedRow}/>
      </TabPanel>
      </>
      )}
      {visibility && (
        <>
      <TabPanel>
        <Tab_EBOM1 isSlideInOpen={isSlideInOpen} setIsSlideInOpen={setIsSlideInOpen} columnsData={columnsData} setColumnsData={setColumnsData}  selectedRowData={selectedRowData} setSelectedRowData={setSelectedRowData} exportButtonRef={exportButtonRef} exportDropdownRef={exportDropdownRef} downloadRef={downloadRef} dropdownVisible={dropdownVisible} setDropdownVisible={setDropdownVisible} ebomAvailableAttributes={ebomAvailableAttributes} setEBOMAvailableAttributes={setEBOMAvailableAttributes} visibleColumns={visibleColumns} dataToExport={dataToExport} exportOption={exportOption} exportDropdownVisible={exportDropdownVisible} setExportDropdownVisible={setExportDropdownVisible} csvLinkKey={csvLinkKey} handleExportOptionChange={handleExportOptionChange} tableData={tableData} setTableData={setTableData} setebomVisibleColumns={setebomVisibleColumns} ebomVisibleColumns={ebomVisibleColumns} selectedRow={selectedRow}/>
      </TabPanel>

      <TabPanel>
        <Tab_Changes isSlideInOpen={isSlideInOpen} setIsSlideInOpen={setIsSlideInOpen} selectedRowData={selectedRowData} setSelectedRowData={setSelectedRowData} columnsData={columnsData} setColumnsData={setColumnsData} caData={caData} setCaData={setCaData} visibleColumns={visibleColumns} setVisibleColumns={setVisibleColumns} downloadRef={downloadRef} availableAttributes={availableAttributes} setAvailableAttributes={setAvailableAttributes} dataToExport={dataToExport} dropdownVisible={dropdownVisible} setDropdownVisible={setDropdownVisible} exportOption={exportOption} csvLinkKey={csvLinkKey} handleExportOptionChange={handleExportOptionChange} exportDropdownVisible={exportDropdownVisible} setExportDropdownVisible={setExportDropdownVisible} selectedRow={selectedRow}/>
      </TabPanel>

      <TabPanel>
        <Tab_Deviation isSlideInOpen={isSlideInOpen} setIsSlideInOpen={setIsSlideInOpen} selectedRowData={selectedRowData} setSelectedRowData={setSelectedRowData} columnsData={columnsData} setColumnsData={setColumnsData} exportButtonRef={exportButtonRef} exportDropdownRef={exportDropdownRef} downloadRef={downloadRef}handleExportOptionChange={handleExportOptionChange} exportDropdownVisible={exportDropdownVisible} setExportDropdownVisible={setExportDropdownVisible} dataToExport={dataToExport} dropdownVisible={dropdownVisible} setDropdownVisible={setDropdownVisible} exportOption={exportOption} csvLinkKey={csvLinkKey} devVisibleColumns={devVisibleColumns} setdevVisibleColumns={setdevVisibleColumns} DevTableData={DevTableData} setDevTableData={setDevTableData} setdevAailableAttributes={setdevAailableAttributes} devAailableAttributes={devAailableAttributes} selectedRow={selectedRow}/>
      </TabPanel>

      {/* Other Tabs */}
      {tabs.slice(5).map((tab, index) => (
        <TabPanel key={index}>
          <h2 className="selected-part-details">{tab.name} content here...</h2>
        </TabPanel>
      ))}
       </>
    )}
    </Tabs>
  );
};
