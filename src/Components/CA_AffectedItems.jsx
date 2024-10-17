import React, { useState, useEffect } from "react";
import DataTable from 'react-data-table-component';
import icon from '../Icons/list.png';
import { Tabs_Sec_Popup } from "./Tabs_Sec_Popup";
import partIcon from '../Icons/PhysicalProduct.png';

export const Affected_popup = ({ isOpen, onClose, rowData, supplierVisibilityValue, specVisibilityValue, itemVisibilityValue, selectedRData, partid, setSelectedRData }) => {
    if (!isOpen) return null;

    return (
        <div className="popup-overlay">
            <div className="popup-content">
                <div className="popup-header">
                    <button className="popup-close-button" onClick={onClose}>
                        X
                    </button>
                </div>
                <div className="data-table-container">
                    <div className="table-wrapper">
                        <Tabs_Sec_Popup
                            supplierVisibilityValue={supplierVisibilityValue}
                            specVisibilityValue={specVisibilityValue}
                            itemVisibilityValue={itemVisibilityValue}
                            partid={partid}
                            selectedRData={selectedRData}
                            setSelectedRData={setSelectedRData  }
                        />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default function CA_AffectedItems({ caId, acknowledgeValue, selectedRData, setSelectedRData, selectedName}) {
    const [tableData, setTableData] = useState([]);
    const [isPopupOpen, setIsPopupOpen] = useState(false);
    const [selectedRowData, setSelectedRowData] = useState(null);
    const [supplierVisibilityValue, setsupplierVisibilityValue] = useState(false);
    const [specVisibilityValue, setspecVisibilityValue] = useState(false);
    const [itemVisibilityValue, setItemVisibilityValue] = useState(false);
    const [collectedIds, setCollectedIds] = useState('');
    const [partid, setpartid] = useState(null);

    useEffect(() => {
        const fetchAffectedItems = async () => {
                try {
                    const response = await fetch(`http://localhost:8081/Supplierportal/webapi/SupplierPortal/getcaaffectedItems?caid=${caId}`);
                    const data = await response.json();
                    const objectArray = data.results || [];
                    const formattedData = [];
                    let supplierVisibility = null;
                    let specVisibility = null;
                    let itemVisibility = null;
                    let allIds = [];
        
                    if (objectArray.length > 0) {
                        objectArray.forEach(objectDetails => {
                            const [key, details] = Object.entries(objectDetails)[0];
        
                            if (details && details.attributes && details.basicAttributes) {
                                const combinedAttribute = {};
                                let partId = null;
        
                                // Extract attributes
                                details.attributes.forEach(attr => {
                                    combinedAttribute[attr.displayName] = attr.value;
                                    if (attr.displayName === 'Id') {
                                        partId = attr.value;
                                        allIds.push(partId);
                                    }
                                });
                                
                                // Extract basic attributes
                                details.basicAttributes.forEach(attr => {
                                    combinedAttribute[attr.displayName] = attr.value;
                                    if(attr.displayName == 'Supplier Visibility') {
                                        supplierVisibility = attr.value === 'ownedPart' ? true : false;
                                    }
                                    if(attr.displayName == 'Supplier Item Visibility'){
                                        specVisibility = attr.value !== "" ? true : false;
                                    } 
                                    if(attr.displayName == 'Supplier Spec Visibility'){
                                        itemVisibility = attr.value !== "" ? true : false;
                                    } 

                                });
    
                                // Set visibility values
                                setsupplierVisibilityValue(supplierVisibility);
                                setspecVisibilityValue(specVisibility);
                                setItemVisibilityValue(itemVisibility);
        
                                // Create formatted data object
                                formattedData.push({
                                    Name: combinedAttribute['Name'] || 'N/A',
                                    Revision: combinedAttribute['Revision'] || 'N/A',
                                    Description: combinedAttribute['Description'] || 'N/A',
                                    State: combinedAttribute['State'] || 'N/A',
                                    Owner: combinedAttribute['Owner'] || 'N/A',
                                    partId: partId
                                });
                            }
                        });
                    }
        
                    // Log formattedData to check extraction
        
                    setTableData(formattedData);
                    const joinedIds = allIds.join('|');
                    setCollectedIds(joinedIds);
        
                    // Fetch Supplier Data
                    const supplierData = await fetchSupplierData(joinedIds, caId);
              
        
                    // Extract supplier attributes
                    const supplierAttributes = supplierData.results?.map(item => {
                        const key = Object.keys(item)[0];
                        return { partId: key.split(": ")[1], ...item[key].attributes[0] };
                    }) || [];
        
                    // Filter formattedData based on supplierAttributes for each partId
                    const filteredData = formattedData.filter(item => {
                        const supplierAttr = supplierAttributes.find(attr => attr.partId === item.partId);
                        const isValid = supplierAttr && (supplierAttr.supplier || supplierAttr.supplieritem || supplierAttr.supplierspec);
                        return isValid;
                    });
        

                    setTableData(filteredData);
                } catch (error) {
                    console.error("Failed to fetch affected items:", error);
                }

        };
        
        

        const fetchSupplierData = async (collectedIds, caId) => {
            const url = 'http://localhost:8081/Supplierportal/webapi/SupplierPortal/getSupplierData';
            const payload = {
                objectIds: collectedIds,
                caid: caId
            };

            try {
                const response = await fetch(url, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    throw new Error(`Error: ${response.statusText}`);
                }

                return await response.json();
            } catch (error) {
                console.error("Failed to fetch supplier data:", error);
            }
        };

        fetchAffectedItems();
    }, [caId, acknowledgeValue]);

    const handleActionClick = async (row) => {
        setSelectedRowData(row);
        setIsPopupOpen(true);

        try {
            const response = await fetch(`http://localhost:8081/Supplierportal/webapi/SupplierPortal/parts?partid=${row.partId}`);
            if (!response.ok) {
                throw new Error(`Error: ${response.statusText}`);
            }
            const rowDetails = await response.json();

            setSelectedRData(rowDetails);
        } catch (error) {
            console.error("Failed to fetch row details:", error);
        }
    };

    const columns = [
        {
            name: 'Name',
            selector: row => row.Name,
            sortable: true,
            cell: row => (
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    <img
                        src={partIcon}
                        alt="PartIcon"
                        style={{ width: '18px', height: '15px', marginRight: '3px' }}
                    />
                    <span>{row.Name}</span>
                </div>
            )
        },
        {
            name: 'Action',
            cell: row => (
                <img
                    src={icon}
                    alt="Action"
                    className="Action"
                    style={{ width: '25px', height: '22px', cursor: 'pointer' }}
                    title="Action"
                    onClick={() => handleActionClick(row)}
                />
            )
        },
        {
            name: 'Revision',
            selector: row => row.Revision,
            sortable: true,
        },
        {
            name: 'Description',
            selector: row => row.Description,
        },
        {
            name: 'State',
            selector: row => row.State,
        },
        {
            name: 'Owner',
            selector: row => row.Owner,
        }
    ];

    return (
        <div className='affecteditemsTable'>
            {acknowledgeValue ? (
                <DataTable
                    columns={columns}
                    data={tableData}
                    pagination
                    highlightOnHover
                />
            ) : (
                <div className="tableErrorMessage">
                    <h4>Selected {selectedName} is not acknowledged. Please acknowledge the corresponding object in order to view the data.</h4>
                </div>
            )}

            <Affected_popup
                isOpen={isPopupOpen}
                onClose={() => setIsPopupOpen(false)}
                rowData={selectedRowData || {}}
                supplierVisibilityValue={supplierVisibilityValue}
                specVisibilityValue={specVisibilityValue}
                itemVisibilityValue={itemVisibilityValue}
                partid={partid}
                selectedRData={selectedRData}
                setSelectedRData={setSelectedRData}
            />
        </div>
    );
}


// Inline styles for the popup (you can replace this with your CSS classes)
const popupStyles = {
    popupOverlay: {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        zIndex: 1000,
    },
    popupContent: {
        backgroundColor: 'white',
        padding: '20px',
        width: '400px',
        height: '300px',
        borderRadius: '10px',
        position: 'relative',
    },
    popupHeader: {
        display: 'flex',
        justifyContent: 'flex-end',
    },
    popupCloseButton: {
        background: 'none',
        border: 'none',
        fontSize: '18px',
        cursor: 'pointer',
    },
    dataTableContainer: {
        marginTop: '20px',
    },
    tableWrapper: {
        overflow: 'auto',
        height: '200px',
    },
};
