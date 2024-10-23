
import React from 'react'
import { useState, useEffect,useContext,useRef} from 'react';
import API_BASE_URL from '../config';
// import { MainContext } from "./MainSection";
import DataTable from 'react-data-table-component';
import columnOrderConfig from './columnOrderConfig.json';
import { SlideInPage } from './SlideInPage';
import ChangeAction from '../Icons/ChangeAction.png'; 
import supplies from '../Icons/supplies.png'; 
import column from '../Icons/Columns.png';
import exporticon from '../Icons/Export.png'; 
import { CSVLink } from 'react-csv';
import partImage from '../Icons/Part.png'; 
import { Tabs_Sec } from './Tabs_Sec';

export const Tab_Changes = ({setIsSlideInOpen,isSlideInOpen,selectedRowData,setSelectedRowData,columnsData,setColumnsData,visibleColumns,setVisibleColumns,availableAttributes,setAvailableAttributes,caData,setCaData,csvLinkKey,exportOption,exportButtonRef,exportDropdownRef,downloadRef,handleExportOptionChange,dropdownVisible,setDropdownVisible,exportDropdownVisible,setExportDropdownVisible,dataToExport,selectedRow}) => {
    // const selectedRow = useContext(MainContext);

 
    const handleChangeActionRowClick = (rowData) => {
        setSelectedRowData(rowData);
        setIsSlideInOpen(true);
      };

      
      const handleColumnSelection = (attr) => {
        if (attr === 'Name'  || attr === 'View Affected Items') {
          return; // Prevent hiding the "Name" column
        }
        setVisibleColumns(prevState => {
          if (prevState.includes(attr)) {
            return prevState.filter(column => column !== attr); // Hide the column
          } else {
            return [...prevState, attr]; // Show the column
          }
        });
      };

   
      const [isPopupOpen, setIsPopupOpen] = useState(false);
      const [popupColumns, setPopupColumns] = useState([]); // Define state for popup columns
      const [popupData, setPopupData] = useState([]); 
      

  

//ChangeAction fetch
const fetchChangeActions = (objectId) => {
    fetch(`${API_BASE_URL}/parts/changeActions?partid=${objectId}`)
    .then(res => res.json())
    .then(data => {

      const objectDetailsArray = data.objectdetails;
  
      if (Array.isArray(objectDetailsArray)) {
        const newAvailableAttributesCA = new Set(['Name', "Policy", "Owner", 'State', 'Synopsis', 'Description', 'View Affected Items']);
        const selectablesList = [];
        const relAttributesList = [];
        const attributesList = [];
        const combinedAttributes = [];
  
        objectDetailsArray.forEach(objectDetails => {
          const [key, details] = Object.entries(objectDetails)[0];
  
          if (details) {
            const selectables = details.BasicAttributesOfChangeAction?.reduce((acc, attr) => {
              acc[attr.displayName] = attr.value;
              newAvailableAttributesCA.add(attr.displayName);
              return acc;
            }, {});
  
            const relAttributes = details.relattributesOfAffectedItems?.reduce((acc, attr) => {
              acc[attr.displayName] = attr.value;
              return acc;
            }, {});
  
            const attributes = details.attributesOfCA?.reduce((acc, attr) => {
              acc[attr.displayName] = attr.value;
              newAvailableAttributesCA.add(attr.displayName);
              return acc;
            }, {});
  
            selectablesList.push(selectables);
            relAttributesList.push(relAttributes);
            attributesList.push(attributes);
  
            combinedAttributes.push({
              ...selectables,
              ...attributes,
              relattributesOfAffectedItems: relAttributes
            });
          } else {
            console.error('No details found for the objectId:', key);
          }
        });
  
        const columnsArray = Array.from(newAvailableAttributesCA).filter(columnName => columnName !== 'View Affected Items');
        const columnsData = columnsArray.map(columnName => ({
          name: columnName,
          selector: row => {
            if (columnName === 'Name' && row[columnName]) {
              return (
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <img src={ChangeAction} alt="Change Action" style={{ width: '20px', height: '20px', marginRight: '8px' }} />
                  {row[columnName]}
                </div>
              );
            }
            return row[columnName] || '-';
          },
          sortable: true,
        }));
  
        const customColumn = {
          name: 'View Affected Items',
          selector: row => (
            <img
              src={supplies}
              alt="View Affected Items"
              className="View Affected Items"
              style={{ width: '20px', height: '20px', cursor: 'pointer' }}
              onClick={() => handleViewAffectedItems(row)}
            />
          ),
          sortable: false,
        };
  
        const updatedColumnsData = [...columnsData, customColumn];
  
        // setSelectables(selectablesList);
        // setRelAttributes(relAttributesList);
        // setAttributes(attributesList);
        setCaData(combinedAttributes);
        setColumnsData(updatedColumnsData);
        setAvailableAttributes([...newAvailableAttributesCA]);
      } else {
        console.error('Expected an array of object details');
      }
      
    })
    .catch(error => {
      console.error('Error fetching CA details:', error);
    });



    // ---------------Affected Items----------------------
    
    const handleViewAffectedItems = (row) => {
      setIsPopupOpen(true);
      
      if (row.relattributesOfAffectedItems) {
        // Extract keys from the relationship attributes to create dynamic columns
        const relAttributeKeys = Object.keys(row.relattributesOfAffectedItems);
        
        // Setup columns dynamically based on the keys from relattributesOfAffectedItems
        const columns = relAttributeKeys.map(attribute => ({
          name: attribute,
          selector: relRow => relRow[attribute] || '-',
          sortable: true,
        }));
    
        // Convert the relattributesOfAffectedItems object to an array of objects
        // Each object represents a row with keys as column names
        const relAttributesArray = [row.relattributesOfAffectedItems];
    
        setPopupColumns(columns);
        setPopupData(relAttributesArray);
      } else {
        console.error('relattributesOfAffectedItems is undefined or null for the selected row:', row);
        setPopupColumns([]);
        setPopupData([]);
      }
    };
  };


  useEffect(() => {
    if (selectedRow) {
        const result = selectedRow.results[0];
        const partDataKey = Object.keys(result).find(key => key.startsWith("objectId:"));
  
        if (partDataKey) {
            const objectId = partDataKey.split("objectId:")[1].trim();
            fetchChangeActions(objectId);
        }
    }
}, [selectedRow]);
  return (
    <>

<div className="dev_header">
            {caData.length > 0 && (
              <>
               <div className='header_right_export'>
                <button ref={exportButtonRef}   onClick={() => setExportDropdownVisible(!exportDropdownVisible)}>
                  <img src={exporticon} alt="Export" />
                </button>
                {exportDropdownVisible && (
                  <div ref={exportDropdownRef} id='exportDropdown' className='dropdown-content'>
                    <ul className='exportdropdown-menu'>
                      <li onClick={() => handleExportOptionChange(true, 'CA')}>
                        Export All Columns
                      </li>
                      <li onClick={() => handleExportOptionChange(false, 'CA')}>
                        Export Current Columns
                      </li>
                    </ul>
                  </div>
                )}
                </div>
                {exportOption !== null && (
                  <CSVLink
                    data={dataToExport.data}
                    headers={dataToExport.headers}
                    filename={`ChangeAction_data_${exportOption ? 'all' : 'current'}.csv`}
                    className="hidden-link"
                    ref={downloadRef}
                    key={csvLinkKey}
                  >
                  </CSVLink>
                )}
                 <div  className="header_right_settings">
                <button ref={exportButtonRef} id='custom' onClick={() => setDropdownVisible(!dropdownVisible)}>
                  <img src={column} alt="Customize Columns" />
                </button>
                {dropdownVisible && (
                  <div ref={exportDropdownRef} id='columnDropdown' className='dropdown-content'>
                    <ul className='dropdown-menu'>
                      {availableAttributes.map(attr => (
                        <li 
                          key={attr} 
                          onClick={() => handleColumnSelection(attr)}
                          className={`${visibleColumns.includes(attr) ? 'selected' : 'unselected'}  ${attr === 'Name' ? 'disabled' : ''}`}
                        >
                          {attr}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                </div>
              </>
            )}
          </div>
     

    <DataTable
        // columns={columnsData}
        columns={columnsData.filter(col => visibleColumns.includes(col.name)) }
        data={caData}
        pagination
        paginationPerPage={6}
        paginationRowsPerPageOptions={[6, 10, 14]}
       
        highlightOnHover
        pointerOnHover
        onRowClicked={handleChangeActionRowClick}
      />

{isSlideInOpen && (
              // <SlideInPage data={selectedRowData} onClose={() => setIsSlideInOpen(false)} />
              <SlideInPage
              data={selectedRowData}
              currentTab={"Changes"} // Pass currentTab to SlideInPage
              fileName= "ChangeAction_Properties.pdf"
              selectedSections = {columnOrderConfig.ChangeActionSlidein}
              onClose={() => setIsSlideInOpen(false)} 
              isSlideInOpen={isSlideInOpen}
              />
            )}  

            <Affected_popup isOpen={isPopupOpen} columns={popupColumns} data={popupData}  onClose={() => setIsPopupOpen(false)} />
    </>
  )
}




// -----------Affected Popup

export const Affected_popup = ({isOpen,columns,data,onClose}) => {
    const [showExportOptions, setShowExportOptions] = useState(false);
    const [showColumnDropdown, setShowColumnDropdown] = useState(false); // Separate state for the column dropdown
    const [selectedColumns, setSelectedColumns] = useState(["Name", "Type", "Revision", "Quantity", "Requested for change"]);
  
    if (!isOpen) return null;
  
    // Mandatory columns
    const mandatoryColumns = ["Name", "Type", "Revision"];
  
    // Function to filter and reorder columns based on the selected order
    const filterAndReorderColumns = (columns, selectedColumns) => {
      const filteredColumns = columns.filter(column => selectedColumns.includes(column.name));
      return filteredColumns.sort((a, b) => selectedColumns.indexOf(a.name) - selectedColumns.indexOf(b.name));
    };
  
    const reorderedColumns = filterAndReorderColumns(columns, selectedColumns);
    
    const updatedColumns = reorderedColumns.map(column => {
      if (column.name === "Name") {
        return {
          ...column,
          cell: (row) => (
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <img src={partImage} alt="Name Icon" style={{ width: '20px', height: '20px', marginRight: '8px' }} />
              {row[column.name]}
            </div>
          )
        };
      }
      return column;
    });
  
    const filteredData = data.map(row => {
      const filteredRow = {};
      selectedColumns.forEach(key => {
        if (row.hasOwnProperty(key)) {
          filteredRow[key] = row[key];
        }
      });
      return filteredRow;
    });
  
    const exportData = (exportAllColumns) => {
      let dataToExport;
      let columnsToExport;
  
      if (exportAllColumns) {
        dataToExport = data;
        columnsToExport = columns;
      } else {
        dataToExport = filteredData;
        columnsToExport = reorderedColumns;
      }
  
      const csvContent = [
        columnsToExport.map(col => col.name).join(','), 
        ...dataToExport.map(row =>
          columnsToExport.map(col => row[col.name] ?? '').join(',')
        ),
      ].join('\n');
  
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.setAttribute('download', `export_${exportAllColumns ? 'all' : 'current'}_columns.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    };
  
    const handleExportOptionChange = (exportAllColumns) => {
      exportData(exportAllColumns);
      setShowExportOptions(false);
    };
  
    const handleColumnSelection = (columnName) => {
      if (mandatoryColumns.includes(columnName)) return; // Prevent unselecting mandatory columns
  
      if (selectedColumns.includes(columnName)) {
        setSelectedColumns(selectedColumns.filter(col => col !== columnName));
      } else {
        setSelectedColumns([...selectedColumns, columnName]);
      }
    };
  
    return (
      <div className="popup-overlay">
        <div className="popup-content">
          <div className="popup-header">
            <div className="popup_Export">
              <button 
                className="popup-command-button" 
                onClick={() => setShowExportOptions(!showExportOptions)}
              >
                <img src={exporticon} alt="Export" title="Export" />
              </button>
  
              {showExportOptions && (
                <div id='exportDropdown' className='dropdown-content-popup'>
                  <ul className='dropdown-menu'>
                    <li onClick={() => handleExportOptionChange(true)}>
                      Export All Columns
                    </li>
                    <li onClick={() => handleExportOptionChange(false)}>
                      Export Current Columns
                    </li>
                  </ul>
                </div>
              )}
            </div>
  
            <div className="popup_Add_Remove_Column">
              <button className="popup-command-button" onClick={() => setShowColumnDropdown(!showColumnDropdown)}>
                <img src={column} alt="Column Add/Remove" title="Column Add/Remove" />
              </button>
  
              {showColumnDropdown && ( // Toggle visibility based on showColumnDropdown
                <div id='columnDropdown' className='dropdown-content-popup-column-addRemove'>
                  <ul className='dropdown-menu'>
                    {columns.map((col, index) => (
                      <li 
                        key={index} 
                        onClick={() => handleColumnSelection(col.name)}
                        style={{ 
                          backgroundColor: selectedColumns.includes(col.name) ? '#e0e0e0' : 'transparent',
                          color: mandatoryColumns.includes(col.name) ? 'red' : 'inherit' // Highlight mandatory columns in red
                        }}
                      >
                        {col.name}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
  
            <button className="popup-close-button" onClick={onClose}>
              X
            </button>
          </div>
          
          <div className="data-table-container">
            <div className="table-wrapper">
              <DataTable
                columns={updatedColumns}
                data={filteredData}
                className="table"
                pagination
                paginationPerPage={4}
                paginationRowsPerPageOptions={[4, 6, 10]}
                highlightOnHover
                pointerOnHover
              />
              {/* <Tabs_Sec/> */}
            </div>
          </div>
        </div>
      </div>
    );
}