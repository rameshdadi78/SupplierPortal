import React, { useState, useEffect } from "react";
import DataTable from 'react-data-table-component';
import acknowledgeIcon from '../Icons/AcknowledgeIcon.png';
import nonAcknowledgeIcon from '../Icons/nonAcknowledgeIcon.png';
import "../Styles/Nav.css";
import DevaitionImage from '../Icons/Deviation.png'
import { Tabs, Tab, TabList, TabPanel } from 'react-tabs';
import jsonStructure from './columnOrderConfig.json'; // Import the JSON structure
// Modal Component
const Modal = ({ isOpen, onClose, children }) => {
    if (!isOpen) return null;

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <button onClick={onClose} className="close-button">X</button>
                {children}
            </div>
        </div>
    );
};

export const Devaition_Page = ({devObjectIds}) => {

    const [acknowledged, setAcknowledgeValue] = useState(false);
    const [tableData, setTableData] = useState([]);
    const [selectedTab, setSelectedTab] = useState(0);
    const [columns, setColumns] = useState([]);
    const [selectedDataTab, setSelectedDataTab] = useState(0);
    const [caCount, setCaCount] = useState(0);
    const [isModalOpen, setIsModalOpen] = useState(false);  // State for modal visibility
    const [selectedRow, setSelectedRow] = useState(null);   // State to store selected row details
    const [acknowledgeState, setAcknowledgeState] = useState(null); // State to determine which icon is clicked
    const [selectedDetails, setSelectedDetails] = useState(null); // State for clicked row details
    const [selectedName, setSelectName] = useState(null);
    const [caId,setCaId]=useState(null);
    const [acknowledgeValue,setAcknowledgedValue] = useState(false);
    const username = localStorage.getItem('username');
    useEffect(() => {
        // Fetch data on component mount
        fetch(`http://localhost:8081/Supplierportal/webapi/SupplierPortal/getDevaitionDetailsByEmail?email=${username}`)
            .then(res => res.json())
            .then(data => {
                const objectArray = data.results || [];
                let acknowledgeValue = null; // Changed from const to let
                const newTableData = [];
                let changeActionCount = 0;
    
                if (objectArray.length > 0) {
                    objectArray.forEach(objectDetails => {
                        const [key, details] = Object.entries(objectDetails)[0];
    
                        if (details) {
                            const combinedAttribute = {};
                            let CAID = ''; // CAID placeholder
    
                            // Combine attributes, excluding relattributes
                            details.attributes?.forEach(attr => {
                                combinedAttribute[attr.displayName] = attr.value;
                              
                                if (attr.displayName === 'CAID') {
                                    CAID = attr.value; // Capture CAID
                                }
                            });
    
                            details.connectionattributes?.forEach(attr => {
                                combinedAttribute[attr.displayName] = attr.value;
                                if (attr.displayName === 'Acknowledge') {
                                    acknowledgeValue = attr.value === 'Yes'; // Set acknowledgeValue as true if 'Yes', else false
                                }
                            });
    
                            // Check if devObjectIds is empty or CAID matches any of the devObjectIds

                            
                            if (devObjectIds.length === 0 || devObjectIds.includes(CAID)) {
                                newTableData.push({
                                    Name: combinedAttribute['Name'] || '',
                                    Description: combinedAttribute['Description'] || '',
                                    Owner: combinedAttribute['Owner'] || '',
                                    acknowledged: acknowledgeValue,
                                    objectId: CAID,
                                    extraData: details,
                                   
                                });
                                
                            }
                        } else {
                            console.error('No Object found:', key);
                        }
                    });
    

                    setTableData(newTableData);
                    setCaCount(changeActionCount);
                } else {
                    console.error('No CAs are there');
                }
            });
    }, [devObjectIds]); // Added devObjectIds as dependency
    

    
    useEffect(() => {
        // Define columns for the DataTable
        const newColumns = [
            {
                name: 'Name',
                selector: row => row.Name,
                sortable: true,
            },
            {
                name: 'Description',
                selector: row => row.Description,
                sortable: true,
            },
            {
                name: 'Owner',
                selector: row => row.Owner,
                sortable: true,
            },
            {
                name: 'Action',
                cell: row => {
                    return (
                        <>
                            {row.acknowledged ? (
                                <>
                                    <img
                                        src={acknowledgeIcon}
                                        alt="AcknowledgeIcon"
                                        className="AcknowledgeIcon"
                                        style={{ width: '32px', height: '27px', cursor: 'pointer' }}
                                        onClick={() => handleIconClick(row, true)}
                                    />
                                </>
                            ) : (
                                <img
                                src={nonAcknowledgeIcon}
                                alt="nonAcknowledgeIcon"
                                className="nonAcknowledgeIcon"
                                style={{ width: '32px', height: '27px', cursor: 'pointer' }}
                                title="Non-Acknowledged"
                                onClick={() => handleIconClick(row, false)} // false for non-acknowledged
                                />
                            )}
                        </>
                    );
                }, 
            },
        ];

        setSelectedTab(0);
        setColumns(newColumns);
    }, []); // Columns effect runs only once

    const handleIconClick = (row, isAcknowledged) => {
        setSelectedRow(row);   // Set the clicked row
        setAcknowledgeState(isAcknowledged); // Set the acknowledgment state
        setIsModalOpen(true);  // Open the modal
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);  // Close the modal
        setSelectedRow(null);   // Clear the selected row
        setAcknowledgeState(null); // Clear the acknowledgment state
    };
    const handleSetData = async() =>{
        if (selectedRow) {
            const value = acknowledged;
            if (username) {
                const response = await updateAcknowledgmentStatus(selectedRow.objectId, value, username);
                if (response.ok) {
                    // Update the tableData to reflect the new acknowledgment status
                    const updatedTableData = tableData.map(row => {

                        if (row.objectId === selectedRow.objectId) {
                            return { ...row, acknowledged: value }; // Update the acknowledged status based on the new value
                        }
                        return row;
                    });
                    setTableData(updatedTableData); 
                    console.log('Acknowledgment status updated successfully');
                } else {
                    console.error('Failed to update acknowledgment status');
                }
            } else {
                console.error('Username not found in local storage');
            }
        }

        setIsModalOpen(false);
        setSelectedRow(null);
        setAcknowledgeState(null);
    }
    const handleRadioChange = (e) => {
        setAcknowledgeValue(e.target.value === "Yes");
    };

    const handleRowClick = async (row) => {
        const objectId = row.objectId;
        const name = row.Name;
        setSelectName(name);
        setSelectedDataTab(objectId);
        const acknowledge = row.acknowledged;
        setCaId(objectId);
        setAcknowledgedValue(acknowledge);

        const details = await fetchChangeActionDetails(objectId);
        
        setSelectedDetails({
            ...details,
            acknowledged: row.acknowledged // Include acknowledgment status
        });

    };
    
    const updateAcknowledgmentStatus = async (objectId, value, username) => {
        try {
            const response = await fetch('http://localhost:8081/Supplierportal/webapi/SupplierPortal/updateAcknowledgedInfo', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    objectId: objectId,
                    value : `${value}`,
                    username : username
                }),
            });

            if (!response.ok) {
                throw new Error('Network response was not ok');
            }

            return response;
        } catch (error) {
            console.error('Error updating acknowledgment status:', error);
            return {};
        }
    };
    const fetchChangeActionDetails = async (objectId) => {
        try {
            const response = await fetch(`http://localhost:8081/Supplierportal/webapi/SupplierPortal/getDevaitionDetails?devid=${objectId}`);
            
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
    
            const data = await response.json();
            const objectData = data.results || [];
            const combinedAttribute = {};
    
            if (objectData.length > 0) {
                objectData.forEach(objectDataDetails => {
                    const [key, details] = Object.entries(objectDataDetails)[0];
    
                    if (details) {
                        details.attributes?.forEach(attr => {
                            combinedAttribute[attr.displayName] = attr.value;
                        });
                    } else {
                        console.error('No Object found:', key);
                    }
                });
    
                return combinedAttribute;
            } else {
                console.error('No results found');
                return {};
            }
        } catch (error) {
            console.error('Error fetching data:', error);
            return {};
        }
    };
    
    const fieldGroups = jsonStructure.Deviation;

    // Fetching data logic...

    // Render fields grouped by section
    const renderGroupedFields = (details) => {
        if (!details) return null; // Safeguard in case details are null
    
        return Object.entries(fieldGroups).map(([groupName, fields]) => (
            <div key={groupName} className="groupHeader">
                <h4>{groupName}</h4>
                <div className="details-grid">
                    {fields.map((field) => (
                        <div className="details-grid-row" key={field}>
                            <div className="details-label">
                                <strong>{field}:</strong>
                            </div>
                            <div className="details-value">
                                {details[field] || "N/A"}
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        ));
    };
    
    

        useEffect(() => {
            if (tableData.length > 0) {
                handleRowClick(tableData[0]); // Automatically handle click on the first row
            }
        }, [tableData]); 
        const conditionalRowStyles = [
            {
                when: row => row.objectId === selectedDataTab,  // Check if the row is selected
                style: {
                    backgroundColor: '#dedede', // Apply highlight color to the selected row
                    color: 'black',              // You can adjust text color if needed
                },
            },
        ];
    return (
        <div>
            <div className="Catable">
                <DataTable
                    columns={columns}
                    data={tableData}
                    pagination
                    highlightOnHover
                    responsive
                    onRowClicked={handleRowClick} 
                    conditionalRowStyles={conditionalRowStyles}
                />
            </div>
            {/* Left-side Tabs */}
            <Tabs selectedIndex={selectedTab} onSelect={index => setSelectedTab(index)}>
                <TabList>
                    <Tab className={selectedTab === 0 ? 'selectedTabColourHandel' : 'nonselectedtabGrayOut'}>
                        <div className="ca-tab-content">
                            <img src={DevaitionImage} className="tab-icon" />
                            <div className="tab-content-size">Devaition Detail(s)</div>
                        </div>
                    </Tab>
                </TabList>
                <TabPanel>
                    {/* Display ChangeAction Details on row click */}
                    {selectedDetails && (
                        <div className="change-action-details">
                            {selectedDetails.acknowledged ? (
                                <div className="caDetails">
                                    {/* Render grouped fields */}
                                    {renderGroupedFields(selectedDetails)}
                                </div>
                            ) : (
                                <div className="caError">
                                    <h4>Selected {selectedName} is not acknowledged. Please acknowledge the corresponding object in order to view the data.</h4>
                                </div>
                            )}
                        </div>
                    )}
                </TabPanel>
            </Tabs>

            <Modal isOpen={isModalOpen} onClose={handleCloseModal}>
                {acknowledgeState ? (
                    <div className="popupMessage">
                        <h4>Object is already acknowledged</h4>
                    </div>
                ) : (
                    <div className="Radiobuttons">
                        <h4>Do you really want to Acknowledge this object</h4>
                        <label>
                            <input
                                type="radio"
                                value="Yes"
                                checked={acknowledged === true}
                                onChange={handleRadioChange}
                            />
                            Yes
                        </label>
                        <label>
                            <input
                                type="radio"
                                value="No"
                                checked={acknowledged === false}
                                onChange={handleRadioChange}
                            />
                            No
                        </label>
                        <button onClick={handleSetData}>OK</button>
                    </div>
                )}
            </Modal>
            {/* <Tabs_Sec/> */}
        </div>
    );
};