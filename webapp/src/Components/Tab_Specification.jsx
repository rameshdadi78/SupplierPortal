import { useState, useEffect,useContext,useRef} from 'react';
// import { MainContext } from "./MainSection";
import DataTable from 'react-data-table-component';
import PartSpecificationDocument from '../Icons/PartSpecificationDocument.png'; 
import { SlideInPage } from './SlideInPage';
import columnOrderConfig from './columnOrderConfig.json';
import { CSVLink } from 'react-csv';
import exporticon from '../Icons/Export.png'; 
import column from '../Icons/Columns.png';

export const Tab_Specification = ({handleExportOptionChange,exportDropdownVisible,setExportDropdownVisible,currentExportType,setIsSlideInOpen,isSlideInOpen,columnsData,setColumnsData,selectedRowData,setSelectedRowData, downloadRef,exportDropdownRef,exportButtonRef,availableAttributes,setAvailableAttributes,exportOption,dataToExport,csvLinkKey,specData,setSpecData,visibleColumns,setVisibleColumns,selectedRow}) => {
    // const selectedRow = useContext(MainContext);



const handleChangeActionRowClick = (rowData) => {
    setSelectedRowData(rowData);
    setIsSlideInOpen(true);
  };

const [dropdownVisible, setDropdownVisible] = useState(false);



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
 

   

    const fetchSpecifications = (objectId) => {
       
        fetch(`http://localhost:8081/Supplierportal/webapi/parts/specifications?partid=${objectId}`)
        .then(res => res.json())
        .then(data => {

        const objectDetailsArray = data.objectdetails;
        if (Array.isArray(objectDetailsArray)) {
          const newAvailableAttributes = new Set(['Name', 'Rev', 'Type', 'Owner', 'State', 'Title', 'Description']);
          const selectablesList = [];
          const relAttributesList = [];
          const attributesList = [];
          const combinedAttributes = [];

          objectDetailsArray.forEach(objectDetails => {
            const [key, details] = Object.entries(objectDetails)[0];

            if (details) {
              const selectables = details.BasicAttributesOfSpecification?.reduce((acc, attr) => {
                acc[attr.displayName] = attr.value;
                newAvailableAttributes.add(attr.displayName);
                return acc;
              }, {});

              const relAttributes = details.relattributesOfSpec?.reduce((acc, attr) => {
                acc[attr.displayName] = attr.value;
                return acc;
              }, {});

              const attributes = details.attributesOfSpec?.reduce((acc, attr) => {
                acc[attr.displayName] = attr.value;
                newAvailableAttributes.add(attr.displayName);
                return acc;
              }, {});

              selectablesList.push(selectables);
              relAttributesList.push(relAttributes);
              attributesList.push(attributes);

              combinedAttributes.push({
                ...selectables,
                ...attributes,
                relattributesOfSpec: relAttributes
              });
            } else {
              console.error('No details found for the objectId:', key);
            }
          });

          const columnsArray = Array.from(newAvailableAttributes);
          const columnsData = columnsArray.map(columnName => ({
            name: columnName,
            selector: row => {
              if (columnName === 'File name' && row[columnName]) {
                return (
                  <span
                    onClick={() => handleFileDownload(row[columnName])}
                    style={{ textDecoration: 'underline', color: 'blue', cursor: 'pointer' }}
                  >
                    {row[columnName]}
                  </span>
                );
              }
              if (columnName === 'Name' && row[columnName]) {
                return (
                  <div style={{ display: 'flex', alignItems: 'center' }}>
                    <img src={PartSpecificationDocument} alt="Specifications" style={{ width: '20px', height: '20px', marginRight: '8px' }} />
                    {row[columnName]}
                  </div>
                );
              }
              return row[columnName] || '-';
            },
            sortable: true,
          }));

        //   setSelectables(selectablesList);
        //   setRelAttributes(relAttributesList);
        //   setAttributes(attributesList);
          setSpecData(combinedAttributes);
          setColumnsData(columnsData);
          setAvailableAttributes([...newAvailableAttributes]);
        } else {
          console.error('Expected an array of object details');
        }
      })
      .catch(error => {
        console.error('Error fetching Specification details:', error);
      });

    };
    useEffect(() => {
        if (selectedRow) {
            const result = selectedRow.results[0];
            const partDataKey = Object.keys(result).find(key => key.startsWith("objectId:"));
      
            if (partDataKey) {
                const objectId = partDataKey.split("objectId:")[1].trim();
                fetchSpecifications(objectId);
            }
        }
    }, [selectedRow]);



    const handleFileDownload = (filename) => {
        // Define the server endpoint to fetch the file
        const serverEndpoint = `http://localhost:8081/Supplierportal/webapi/parts/download?filename=${filename}`;
      
        fetch(serverEndpoint)
          .then(response => {
            if (!response.ok) {
              throw new Error('Network response was not ok');
            }
            return response.blob();
          })
          .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);
          })
          .catch(error => {
            console.error('Error downloading file:', error);
          });
      };
      


  return (
    <>
      <div className="dev_header">
            {specData.length > 0 && (
              <>
              <div className='header_right_export'>
                <button ref={exportButtonRef}   onClick={() => setExportDropdownVisible(!exportDropdownVisible)}>
                  <img src={exporticon} alt="Export" />
                </button>
                {exportDropdownVisible && (
                  <div  ref={exportDropdownRef} id='exportDropdown' className='dropdown-content'>
                    <ul className='exportdropdown-menu'>
                      <li onClick={() => handleExportOptionChange(true, 'Spec')}>
                        Export All Columns
                      </li>
                      <li onClick={() => handleExportOptionChange(false, 'Spec')}>
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
                    filename={`Specification_data_${exportOption ? 'all' : 'current'}.csv`}
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
        columns={columnsData.filter(col => visibleColumns.includes(col.name)) }
        data={specData}
        pagination
        paginationPerPage={6}
        paginationRowsPerPageOptions={[6, 10, 14]}
        highlightOnHover
        pointerOnHover
        onRowClicked={handleChangeActionRowClick}
      />
   {isSlideInOpen && (
                      <SlideInPage
                      data={selectedRowData}
                      currentTab={"Spec"} // Pass currentTab to SlideInPage
                      fileName= "Specification_Properties.pdf"
                      selectedSections = {columnOrderConfig.SpecificationSlidein}
                      onClose={() => setIsSlideInOpen(false)} 
                      isSlideInOpen={isSlideInOpen}/>
         )}
        
    </>
  )
}
