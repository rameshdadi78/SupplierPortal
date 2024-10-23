
import React from 'react'
import { useState, useEffect,useContext,useRef,useCallback} from 'react';
import API_BASE_URL from '../config';
// import { MainContext } from "./MainSection";
import DataTable from 'react-data-table-component';
import columnOrderConfig from './columnOrderConfig.json';
import { SlideInPage } from './SlideInPage';
import { CSVLink } from 'react-csv';
import Deviation from '../Icons/Deviation.png';
import propertiesicon from '../Icons/Properties.png';
import exporticon from '../Icons/Export.png'; 
import column from '../Icons/Columns.png';

export const Tab_Deviation = ({setIsSlideInOpen,isSlideInOpen,selectedRowData,setSelectedRowData,columnsData,setColumnsData,exportButtonRef,handleExportOptionChange,exportDropdownVisible,setExportDropdownVisible,exportDropdownRef,downloadRef,dataToExport,dropdownVisible,setDropdownVisible,exportOption,csvLinkKey,devVisibleColumns,setdevVisibleColumns,DevTableData,setDevTableData,setdevAailableAttributes,devAailableAttributes,selectedRow}) => {
    // const selectedRow = useContext(MainContext);



    const handleDeviationRowClick = (rowData) => {
        setSelectedRowData(rowData);
        setIsSlideInOpen(true);
      };

      //-------------------Export-------------



      useEffect(() => {
        const attributes = new Set();
        DevTableData.forEach(item => {
          Object.keys(item).forEach(key => attributes.add(key));
        });
      
        const generatedColumns = Array.from(attributes).map(attr => {
          if (attr === 'Name') {
            return {
              name: 'Name',
              selector: row => (
                <div className='TableDevIcon' style={{ display: 'flex', alignItems: 'center' }}>
                  <img src={Deviation} alt="Deviation Icon" style={{ marginRight: '5px' }} />
                  <span>{row[attr] || 'N/A'}</span>
                </div>
              ),
              sortable: true
            };
          }
          return {
            name: attr,
            selector: row => row[attr] || 'N/A', // Display 'N/A' if the attribute is missing
            sortable: true
          };
        });
      

      
        setColumnsData(generatedColumns);
      }, [DevTableData]);


      const fetchDeviation = (objectId) => {
        //Deviation fetch code starts
        fetch(`${API_BASE_URL}/portaldata/deviationdetails?partid=${objectId}`)
        .then(res => res.json())
        .then(data => {

        const objectDetailsArray = data.objectdetails;
        if (Array.isArray(objectDetailsArray)) {
            if (objectDetailsArray.length > 0)  {
            const combinedDataList = [];
            const newAvailableAttributes = new Set(['Name', 'Title', 'Description', 'State']); // Default attributes

            objectDetailsArray.forEach(objectDetails => {
            const [key, details] = Object.entries(objectDetails)[0];

            if (details) {
                
                const combinedAttributes = {};
                const partid =  details.partid;
                // Combine selectables and attributes, exclude relattributes
                details.selectables?.forEach(attr => {
                combinedAttributes[attr.displayname] = attr.value;
                newAvailableAttributes.add(attr.displayname);
                });

                details.attributes?.forEach(attr => {
                combinedAttributes[attr.displayname] = attr.value;
                newAvailableAttributes.add(attr.displayname);
                });

                combinedDataList.push(combinedAttributes);
            } else {
                console.error('No details found for the objectId:', key);
            }
            });
            setDevTableData(combinedDataList);
            setdevAailableAttributes([...newAvailableAttributes]);


        } else {
            console.error('No beviation for this part');
            setDevTableData([]);
            setdevAailableAttributes([]);
        }
        } else {
            console.error('Expected an array of object details');
            setDevTableData([]);
            setdevAailableAttributes([]);
        }
        })
        .catch(error => {
        console.error('Error fetching deviation details:', error);
        });
    };


    useEffect(() => {
        if (selectedRow) {
            const result = selectedRow.results[0];
            const partDataKey = Object.keys(result).find(key => key.startsWith("objectId:"));
      
            if (partDataKey) {
                const objectId = partDataKey.split("objectId:")[1].trim();
                fetchDeviation(objectId);
            }
        }
    }, [selectedRow]);

    const handleDevColumnSelection = (attr) => {
        if (attr === 'Name') {
          return; // Prevent hiding the "Name" column
        }
        setdevVisibleColumns(prevState => {
          if (prevState.includes(attr)) {
            return prevState.filter(column => column !== attr); // Hide the column
          } else {
            return [...prevState, attr]; // Show the column
          }
        });
      };
      
    return (
   <>

<div className="dev_header">
            {DevTableData.length > 0 && (
              <>
                 <div className='header_right_export'>
                <button ref={exportButtonRef} onClick={() => setExportDropdownVisible(!exportDropdownVisible)}>
                  <img src={exporticon} alt="Export" />
                </button>
                {exportDropdownVisible && (
                  <div ref={exportDropdownRef} id='exportDropdown' className='dropdown-content'>
                    <ul className='exportdropdown-menu'>
                      <li onClick={() => handleExportOptionChange(true, 'Deviation')}>
                        Export All Columns
                      </li>
                      <li onClick={() => handleExportOptionChange(false, 'Deviation')}>
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
                    filename={`deviation_data_${exportOption ? 'all' : 'current'}.csv`}
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
                      {devAailableAttributes.map(attr => (
                        <li 
                          key={attr} 
                          onClick={() => handleDevColumnSelection(attr)}
                          className={`${devVisibleColumns.includes(attr) ? 'selected' : 'unselected'}  ${attr === 'Name' ? 'disabled' : ''}`}
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
            columns={columnsData.filter(col => devVisibleColumns.includes(col.name))}
            data={DevTableData}
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
            onRowClicked={handleDeviationRowClick}
          />

{isSlideInOpen && (
            <SlideInPage data={selectedRowData}  
            currentTab={"Deviation"}  
            fileName= "Deviation_Properties.pdf" 
            selectedSections = {columnOrderConfig.Deviationsections}
            onClose={() => setIsSlideInOpen(false)} 
            isSlideInOpen={isSlideInOpen}
            />
          )}
   </>
  )
}
