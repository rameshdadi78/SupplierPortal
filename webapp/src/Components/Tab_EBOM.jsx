import DataTable from 'react-data-table-component';
import { useState,useEffect,useContext } from 'react';
import API_BASE_URL from '../config';
import { MainContext } from "./MainSection";
import React from 'react'
import partIcon from '../Icons/PLMInsertNewProduct.png'
import maximizeIcon from '../Icons/maximize.png'
import minimizeIcon from '../Icons/minimize.png'
import expandIcon from '../Icons/expandIcon.png';
import exporticon from '../Icons/Export.png'; 
import { CSVLink } from 'react-csv';
import columnOrderConfig from './columnOrderConfig.json';
import { SlideInPage } from './SlideInPage';
import column from '../Icons/Columns.png';



export const Tab_EBOM = ({setIsSlideInOpen,isSlideInOpen,columnsData,setColumnsData, selectedRowData,setSelectedRowData,exportButtonRef,exportDropdownRef,downloadRef,dropdownVisible,setDropdownVisible,ebomAvailableAttributes,setEBOMAvailableAttributes,visibleColumns,dataToExport,exportOption,exportDropdownVisible,setExportDropdownVisible,csvLinkKey,handleExportOptionChange,tableData,setTableData,setebomVisibleColumns,ebomVisibleColumns}) => {
    const selectedRow = useContext(MainContext);

  

  
    const [expandedRows, setExpandedRows] = useState(new Set());
    const [isAllExpanded, setIsAllExpanded] = useState(false); 
    const [allRowsExpanded, setAllRowsExpanded] = useState(false);
    const [loading, setLoading] = useState(false);
    
    const handleEBOMRowClick = (rowData) => {
        setSelectedRowData(rowData);
        setIsSlideInOpen(true);
      };


    const [currentObjectid, setCurrentObjectid]= useState(null);
  
    const fetchEBOMdetails = async (objectId, selectedRow) => {
     
        // Set current objectId and reset data if it's a different object
        if (objectId !== currentObjectid) {
            setCurrentObjectid(objectId);
            setTableData([]);
        }
    
        try {
            // Extract part data from selected row
            const result = selectedRow.results[0];
            const partDataKey = Object.keys(result).find(key => key.startsWith("objectId:"));
            
            if (partDataKey) {
                const partObjectId = partDataKey.split("objectId:")[1].trim();            
                const partData = result[partDataKey];
    
                // Extract basic and other attributes
                const basicAttributes = partData.basicAttributes?.reduce((acc, attr) => {
                    acc[attr.displayName] = attr.value;
                    return acc;
                }, {}) || {};
    
                const otherAttributes = partData.attributes?.reduce((acc, attr) => {
                    acc[attr.displayName] = attr.value;
                    return acc;
                }, {}) || {};
    
                // Merge attributes into parentAttributes
                const parentAttributes = { ...basicAttributes, ...otherAttributes, objectId: partObjectId };

                
    
                // Initialize combined data list with parent attributes
                const combinedDataList = [
                    {
                        ...parentAttributes,
                        isChild: false,
                        depth: 0, // Parent item
                    },
                ];
    
                // Fetch EBOM details for the selected part
                const response = await fetch(`${API_BASE_URL}/SupplierPortal/ebomdetails?fromid=${partObjectId}&connattributes=true`);
                const ebomData = await response.json();
                const parentDetailsArray = ebomData.results;
    
                if (Array.isArray(parentDetailsArray) && parentDetailsArray.length > 0) {
                    const newAvailableAttributes = new Set(['Name', 'Description', 'Owner']); // Default attributes
    

                    parentDetailsArray.forEach((parentDetails) => {
                        const [key, details] = Object.entries(parentDetails)[0];
    
                        if (details) {
                            const childAttributes = {};
                            let childObjectId = null;
    
                            // Process child attributes
                            details.attributes?.forEach((attr) => {
                                childAttributes[attr.displayName] = attr.value;
                                newAvailableAttributes.add(attr.displayName);
                                if (attr.displayName === 'Id') {
                                    childObjectId = attr.value;
                                }
                            });
    
                            // Process basic attributes
                            details.basicattributes?.forEach((attr) => {
                                childAttributes[attr.displayName] = attr.value;
                                newAvailableAttributes.add(attr.displayName);
                            });
    
                            // Process connection attributes
                            details.connectionattributes?.forEach((attr) => {
                                childAttributes[attr.displayName] = attr.value;
                                newAvailableAttributes.add(attr.displayName);
                            });
    
                            // Add child data to the combined list
                            combinedDataList.push({
                                ...childAttributes,
                                objectId: childObjectId,
                                isChild: true,
                                depth: 1, // Child item
                            });
                        }
                    });
    
                    // Update state after processing parent and child data
                    setTableData(combinedDataList);
                    setEBOMAvailableAttributes([...newAvailableAttributes]);
                } else {
                    console.error('No EBOM data for this part');
                    setTableData(combinedDataList); // Show parent data even if no child data
                    setEBOMAvailableAttributes([]);
                }
            }
        } catch (error) {
            console.error('Error fetching EBOM details:', error);
        }
    };
    // Reset table data and fetch EBOM details on row selection
    useEffect(() => {
        if (selectedRow) {
            const result = selectedRow.results[0];
            const partDataKey = Object.keys(result).find(key => key.startsWith("objectId:"));
    
            if (partDataKey) {
                const objectId = partDataKey.split("objectId:")[1].trim();
                fetchEBOMdetails(objectId, selectedRow);
            }
        }
    }, [selectedRow]); // Depend on selectedRow
    
    useEffect(() => {
        const attributes = new Set();
      
        tableData.forEach(item => {
          Object.keys(item).forEach(key => attributes.add(key));
        });
      
        const attributesArray = Array.from(attributes).filter(attr => attr !== 'Name');
        const EBOMIcon = partIcon;
        const generatedColumns = [
          {
            name: 'Name',
            selector: (row) => {
              
              const isExpanded = expandedRows.has(row.objectId); // Check if the row is expanded
      
              return (
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    paddingLeft: `${(row.depth || 0) * 20}px`, 
                  }}
                >
                  <img
                    src={isExpanded ? minimizeIcon : maximizeIcon} 
                    alt={isExpanded ? 'Collapse Icon' : 'Expand Icon'}
                    style={{ width: '14px', height: '14px', cursor: 'pointer' }}
                    onClick={() => toggleRowExpansion(row.objectId)}
                  />
                  <img src={EBOMIcon} alt="Part Icon" style={{ width: '20px', height: '20px' }} />
                  {row.Name}
                </div>
              );
            },
            sortable: true,
          },
          ...attributesArray.map(attr => ({
            name: attr,
            selector: row => row[attr] ,
            sortable: true,
          })),
        ];
        
        setColumnsData(generatedColumns);
      }, [tableData, expandedRows]);
      

      const fetchChildData = async (parentObjectId) => {
        try {
          const response = await fetch(`${API_BASE_URL}/SupplierPortal/ebomdetails?fromid=${parentObjectId}&connattributes=true`);
          const data = await response.json();
      
          if (data.results && Array.isArray(data.results)) {
            const childRows = [];
      
            data.results.forEach((childDetails) => {
              const [childKey, childDetailsObj] = Object.entries(childDetails)[0];
              if (childDetailsObj) {
                const childAttributes = {};
                let childObjectId = null;
                
      
                childDetailsObj.attributes?.forEach((attr) => {
                  childAttributes[attr.displayName] = attr.value;
                  if (attr.displayName === 'Id') {
                    childObjectId = attr.value;
                  }
                });
      
                childDetailsObj.basicattributes?.forEach((attr) => {
                  childAttributes[attr.displayName] = attr.value;
                });
      
                childDetailsObj.connectionattributes?.forEach((attr) => {
                  childAttributes[attr.displayName] = attr.value;
                });
      
                childAttributes.objectId = childObjectId;
                childAttributes.parentObjectId = parentObjectId;
                childAttributes.depth = (tableData.find(row => row.objectId === parentObjectId)?.depth || 0) + 1;
                childAttributes.isChild = true;
               
      
                if (childObjectId) {
                  childRows.push(childAttributes);
                }
              }
            });
      
            return childRows;
          } else {
            console.warn('Unexpected data format or empty results:', data);
            return [];
          }
        } catch (error) {
          console.error('Error fetching child details:', error);
          return [];
        }
      };


      const toggleRowExpansion = async (objectId) => {
        const newSet = new Set(expandedRows); // Copy the current set
      
        if (newSet.has(objectId)) {
          // Collapse the row
          newSet.delete(objectId);
          setTableData(prevData =>
            prevData.filter(row => row.parentObjectId !== objectId)
          );
        } else {
          // Expand the row
          newSet.add(objectId);
          await fetchDirectChildren(objectId); // Fetch children first
        }
      
        // Update the state with the new set
        setExpandedRows(newSet);
      
        // Check if all rows are expanded or collapsed and update accordingly
        if (newSet.size !== tableData.length) {
          setIsAllExpanded(false);
          setAllRowsExpanded(false);
        }
      };




// Fetch and add only direct children
const fetchDirectChildren = async (parentObjectId) => {
    const response = await fetch(`${API_BASE_URL}/SupplierPortal/ebomdetails?fromid=${parentObjectId}&connattributes=true`);
    const childData = await response.json();
    const childDetailsArray = childData.results;
  
    if (Array.isArray(childDetailsArray) && childDetailsArray.length > 0) {
      setTableData(prevData => {
        const newData = [...prevData];
        const existingObjectIds = new Set(prevData.map(row => `${row.objectId}_${row.parentObjectId}`));
  
        childDetailsArray.forEach(childDetails => {
          const [childKey, childDetailsObj] = Object.entries(childDetails)[0];
          if (childDetailsObj) {
            const childAttributes = {};
            let childObjectId = null;
            
            childDetailsObj.attributes?.forEach(attr => {
              childAttributes[attr.displayName] = attr.value;
              if (attr.displayName === 'Id') {
                childObjectId = attr.value;
              }
            });
  
            childDetailsObj.basicattributes?.forEach(attr => {
              childAttributes[attr.displayName] = attr.value;
            });
  
            childDetailsObj.connectionattributes?.forEach(attr => {
              childAttributes[attr.displayName] = attr.value;
            });
  
            childAttributes.objectId = childObjectId;
            childAttributes.parentObjectId = parentObjectId;
            childAttributes.depth = (prevData.find(row => row.objectId === parentObjectId)?.depth || 0) + 1;
            childAttributes.isChild = true;
           
            const uniqueKey = `${childObjectId}_${parentObjectId}`;
  
            if (!existingObjectIds.has(uniqueKey)) {
              newData.push(childAttributes);
              existingObjectIds.add(uniqueKey); // Track the unique combination
            }
          }
        });
  
        return newData;
      });
    }
  };
  
      const renderRows = () => {
        const rows = [];
      
        const renderRowAndChildren = (row) => {
          rows.push(row);
      
          const childRows = tableData.filter(
            (child) => child.parentObjectId === row.objectId
          );
          childRows.forEach(renderRowAndChildren);
        };
      
        tableData.forEach((row) => {
          if (!row.parentObjectId) {
            renderRowAndChildren(row);
          }
        });
      
        return rows;
      };
   
      const expandAllChildren = async () => {
    
        if (isAllExpanded) {
          // Collapse all rows
          setExpandedRows(new Set()); 
          setTableData(prevData => prevData.filter(row => !row.parentObjectId));
          setIsAllExpanded(false);
          setAllRowsExpanded(false); // Update state to indicate no rows are expanded
        } else {
          setLoading(true);
          
          // Expand all rows logic
           const fetchAndExpandAll = async (parentObjectId, currentDepth) => {
            const childRows = await fetchChildData(parentObjectId);
            if (childRows.length > 0) {
              setTableData(prevData => {
                const newData = [...prevData];
                const existingObjectIds = new Set(prevData.map(row => `${row.objectId}_${row.parentObjectId}`));
                
                childRows.forEach(child => {
                  const uniqueKey = `${child.objectId}_${parentObjectId}`;
                  if (!existingObjectIds.has(uniqueKey)) {
                    child.depth = currentDepth; 
                    newData.push(child);
                    fetchAndExpandAll(child.objectId, currentDepth + 1); // Recursively fetch and expand
                    existingObjectIds.add(uniqueKey); // Track the unique combination
                  }
                });
          
                return newData;
              });
          
              setExpandedRows(prev => new Set([...prev, ...childRows.map(child => child.objectId)]));
            }
          };
      
          const topLevelRows = tableData.filter(row => !row.parentObjectId);
          for (const topLevelRow of topLevelRows) {
            await fetchAndExpandAll(topLevelRow.objectId, 1);
          }
      
          // Update parent rows to be expanded as well
          const allRowIds = new Set(tableData.map(row => row.objectId));
          setExpandedRows(prev => new Set([...prev, ...allRowIds]));
      
          setLoading(false);
          setIsAllExpanded(true);
          setAllRowsExpanded(true); // Update state to indicate all rows are expanded
        }
      };


      const handleEbomColumnSelection = (attr) => {
        if (attr === 'Name') {
          return; // Prevent hiding the "Name" column
        }
        setebomVisibleColumns(prevState => {
          if (prevState.includes(attr)) {
            return prevState.filter(column => column !== attr); // Hide the column
          } else {
            return [...prevState, attr]; // Show the column
          }
        });
      };









  return (
   <>

 <div className="EBOM_header">
              {tableData.length > 0 && (
                <>
  <div className='header_right_export'>
         <button onClick={expandAllChildren} disabled={loading} className="expand_button">
             <img src={expandIcon} alt={isAllExpanded ? "Collapse All" : "Expand All"} title={isAllExpanded ? "Collapse All" : "Expand All"} />
          </button>

              <button ref={exportButtonRef}   onClick={() => setExportDropdownVisible(!exportDropdownVisible)}>
                <img src={exporticon} alt="Export" />
              </button>
              
              {exportDropdownVisible && (
                <div ref={exportDropdownRef} id="exportDropdown" className="dropdown-content">
                  <ul className="exportdropdown-menu">
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
              {exportOption !== null && (
                <CSVLink
                  data={dataToExport.data}
                  headers={dataToExport.headers}
                  filename={`EBOM_data_${exportOption ? 'all' : 'current'}.csv`}
                  className="hidden-link" // Hide the link visually
                  ref={downloadRef}
                  key={csvLinkKey} // Use a unique key to force re-render
                />
              )}
 <div  className="header_right_settings">
              <button  ref={exportButtonRef} id="custom" onClick={() => setDropdownVisible(!dropdownVisible)}>
                <img src={column} alt="Customize Columns" />
              </button>

              {dropdownVisible && (
                <div  ref={exportDropdownRef} id="columnDropdown" className="dropdown-content">
                  <ul className="dropdown-menu">
                    {ebomAvailableAttributes.filter((attr) => attr !== 'Id') // Filter out the 'id' attribute
                        .map((attr) => (
                          <li
                            key={attr}
                            onClick={() => handleEbomColumnSelection(attr)}
                            className={`${visibleColumns.includes(attr) ? 'selected' : 'unselected'} ${attr === 'Name' ? 'disabled' : ''}`}
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
            columns={columnsData.filter((col) => ebomVisibleColumns.includes(col.name))}
            data={renderRows()} // Use the renderRows function to manage data
            pagination
            paginationPerPage={6}
            paginationRowsPerPageOptions={[6, 10, 14]}
            highlightOnHover
            pointerOnHover
            customStyles={{
              rows: {
                style: {
                  backgroundColor: 'white',
                },
              },
            }}
            expandableRowsComponent={({ data }) => <pre>{JSON.stringify(data, null, 2)}</pre>}
            onRowClicked={handleEBOMRowClick}
          />

          
        {isSlideInOpen && (
              <SlideInPage
                data={selectedRowData}
                currentTab={"EBOM"}
                fileName= "EBOM_Properties.pdf"
                selectedSections = {columnOrderConfig.EBOMSlidein}
                onClose={() => setIsSlideInOpen(false)}
                isSlideInOpen={isSlideInOpen}
              />
          )}
   </>
  )
}
