import { useRef } from "react";
import { Data_Table } from "./Data_Table";
import { createContext, useContext, useEffect, useState } from "react";
import { Filter } from "./Filter";
import { Tabs_Sec } from "./Tabs_Sec";
import "../Styles/MainSection.css";
import columnOrderConfig from './columnOrderConfig.json';
import { FirstContext } from "../App";

export const MainContext = createContext();

const MainSection = ({ setSelectedRow, selectedRow, setSupplierName, visiblePartIds,visibleSpec, visibleItem}) => {
    const userData = JSON.parse(localStorage.getItem('userData')) || {};
    const jwtToken = userData.jwt;
    const initialUserPre = userData.preference || {};

    const [data, setData] = useState([]);
    const [filteredData, setFilteredData] = useState([]);
    const [displayNameMapping, setDisplayNameMapping] = useState({});
    const [filterText, setFilterText] = useState('');
    const [filterParams, setFilterParams] = useState({ column: '', operator: 'contains', value: '' });
    const [rowsPerPage, setRowsPerPage] = useState(6);
    const [userPre, setUserPre] = useState(initialUserPre);
    const [currentViewData, setCurrentViewData] = useState(userPre.views.main_table_view[0].name);
    const username = localStorage.getItem('username');
    const [selectedRowId, setSelectedRowId] = useState(null);
    const [Visibility, setVisibility] = useState('');
    const [specVisibility, setSpecVisibility] = useState('');
    const [itemVisibility, setitemVisibility] = useState('');
  
    useEffect(() => {
        const fetchSupplierName = async () => {
            try {
                const response = await fetch(`http://localhost:8081/Supplierportal/webapi/SupplierPortal/getSupplierName?username=${username}`);
                const result = await response.json();
                setSupplierName(result.results[0].suppliername);
                return result.results[0].suppliername;
            } catch (error) {
                console.error('Error fetching supplier name:', error);
            }
        };

        const fetchVisibility = async (partId, supplierName) => {
            try {
                const response = await fetch(`http://localhost:8081/Supplierportal/webapi/SupplierPortal/getAssignedParts?suppliername=${supplierName}&partid=${partId}`);
                const result = await response.json();
                
                return result.results[0].Visibility;
            } catch (error) {
                console.error('Error fetching visibility:', error);
                return false;
            }
        };

        const fetchData = async () => {
            try {
                const supplierName = await fetchSupplierName();
                const response = await fetch('http://localhost:8081/Supplierportal/webapi/SupplierPortal/parts');
                const fetchDatas = await response.json();

                const visiblePartIdsArray = Array.isArray(visiblePartIds) ? visiblePartIds : [];
                const includedObjectIds = new Set();
                const combinedData = await Promise.all(fetchDatas.results.map(async data => {
                    const key = Object.keys(data)[0];
                    const objId = key.split(':')[1].trim();
                    let visibilityValues =('');
                    const basicAttributes = data[key].basicAttributes.reduce((acc, attr) => {
                        acc[attr.name] = attr.value;
                        return acc;
                    }, {});

                    const attributes = data[key].attributes.reduce((acc, attr) => {
                        acc[attr.name] = attr.value;
                        return acc;
                    }, {});

                //     const visibilityValue = await fetchVisibility(objId, supplierName);
                    
                   
                //     if (visibilityValue === 'true' && (visiblePartIdsArray.length === 0 || visiblePartIdsArray.includes(objId))) {
                //         visibilityValues=true;
                //         if (!includedObjectIds.has(objId)) {
                //             includedObjectIds.add(objId);
                //             return { objectId: objId, ...basicAttributes, ...attributes };
                            
                //         } 
                       
                //     } else if(visibilityValue === 'false' && visiblePartIdsArray.includes(objId)){
                //         if (!includedObjectIds.has(objId)) {
                //             includedObjectIds.add(objId);
                //             return { objectId: objId, ...basicAttributes, ...attributes };
                //         } 
                //         visibilityValues=false;
                //     }
                //     console.log("visibilityValues---------",visibilityValues);
                    
                //     setVisibility(visibilityValues);
                //     return null;
                // })
                const visibilityValue = await fetchVisibility(objId, supplierName);
                const isVisible = visibilityValue === 'true';
                const isPartIncluded = visiblePartIdsArray.includes(objId);
                    
                
                    
                if (
                    (isVisible && (visiblePartIdsArray.length === 0 || isPartIncluded)) ||
                    (!isVisible && isPartIncluded)
                ) {
                    if (!includedObjectIds.has(objId)) {
                        includedObjectIds.add(objId);
                        setVisibility(isVisible); // Update visibility state
                        setSpecVisibility(visibleSpec);
                        setitemVisibility(visibleItem);
                        return { objectId: objId, ...basicAttributes, ...attributes };
                    }
                }
                return null;
            })
                );

                const filteredCombinedData = combinedData.filter(part => part !== null);
                setData(filteredCombinedData);
                setFilteredData(filteredCombinedData);

                const displayNames = fetchDatas.results.reduce((acc, data) => {
                    const key = Object.keys(data)[0];
                    const basicAttributes = data[key].basicAttributes.reduce((accInner, attr) => {
                        accInner[attr.name] = attr.displayName;
                        return accInner;
                    }, {});

                    const attributes = data[key].attributes.reduce((accInner, attr) => {
                        accInner[attr.name] = attr.displayName;
                        return accInner;
                    }, {});

                    return { ...acc, ...basicAttributes, ...attributes };
                }, {});

                const orderedDisplayNames = columnOrderConfig.columnOrder.reduce((acc, key) => {
                    if (displayNames[key]) {
                        acc[key] = displayNames[key];
                    }
                    return acc;
                }, {});

                setDisplayNameMapping(orderedDisplayNames);
            } catch (error) {
                console.error('Error fetching data:', error);
            }
        };

        fetchData();
    }, [visiblePartIds]);

    // Auto-select first row on `filteredData` update
    useEffect(() => {
        if (filteredData.length > 0) {
            handleRowClicked({ data: filteredData[0] }); // Automatically click the first row
        }
    }, [filteredData]);

    useEffect(() => {
        let updatedData = [...data];
        if (filterParams.column) {
            updatedData = updatedData.filter(item => {
                const value = item[filterParams.column];
                if (value !== undefined && value !== null) {
                    const lowerValue = value.toString().toLowerCase();
                    const lowerFilterValue = filterParams.value.toLowerCase();

                    switch (filterParams.operator) {
                        case 'contains':
                            return lowerValue.includes(lowerFilterValue);
                        case 'equals':
                            return lowerValue === lowerFilterValue;
                        case 'startsWith':
                            return lowerValue.startsWith(lowerFilterValue);
                        default:
                            return false;
                    }
                }
                return false;
            });
        }

        if (filterText) {
            updatedData = updatedData.filter(item =>
                Object.values(item).some(val =>
                    val.toString().toLowerCase().includes(filterText.toLowerCase())
                )
            );
        }
        setFilteredData(updatedData);
    }, [data, filterParams, filterText]);

    const columns = data.length > 0
        ? columnOrderConfig.columnOrder
            .filter((key) => currentViewData.includes(key))
            .map((key) => ({
                field: key,
                headerName: displayNameMapping[key] || key,
                filter: true,
                tooltipField: key,
            }))
        : [];

    const handleFilter = (filterParams) => {
        setFilterParams(filterParams);
    };

    const handleSearch = (searchText) => {
        setFilterText(searchText);
    };

    const filterDataForExport = (dataToExport) => {
        dataToExport = Array.isArray(dataToExport) && dataToExport.length > 0 ? dataToExport : [];

        return dataToExport.map(row => {
            const filteredRow = {};

            if (row && typeof row === 'object') {
                Object.keys(row).forEach(key => {
                    filteredRow[key] = row[key];
                });
            } else {
                console.warn('Invalid row encountered:', row);
            }

            return filteredRow;
        });
    };

    const generateCsvHeaders = (columnDisplayNames) => {
        return Object.entries(columnDisplayNames)
            .map(([key, displayName]) => ({
                label: displayName,
                key: key,
            }));
    };

    const exportSpecificRows = (count) => {
        return data.slice(0, count);
    };

    const getRowId = (params) => {
        return params.data.objectId;
    };

    const handleRowClicked = async (event) => {
        const objectId = event.data.objectId;
        setSelectedRowId(objectId)
        localStorage.setItem("is_Active", "true");

        try {
            const response = await fetch(`http://localhost:8081/Supplierportal/webapi/SupplierPortal/parts?partid=${objectId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    "jwt": jwtToken,
                },
            });
            const rowDetails = await response.json();
            setSelectedRow(rowDetails);

        } catch (error) {
            console.error('Error fetching row details:', error);
        }
    };

    const isActive = localStorage.getItem("is_Active");
    const intervalRef = useRef(null);
    const timeoutRef = useRef(null);

    const restartTimer = () => {
        clearInterval(intervalRef.current);
        clearTimeout(timeoutRef.current);

        intervalRef.current = setInterval(logMessage, 900000);
        timeoutRef.current = setTimeout(() => {
            clearInterval(intervalRef.current);

        }, 1800000);
    };

    const stopTimer = () => {
        clearInterval(intervalRef.current);
        clearTimeout(timeoutRef.current);

    };

    useEffect(() => {
        if (isActive) {
            restartTimer();
        } else {
            console.log("Component is inactive");
        }
    }, []);

    const logMessage = async () => {
        const currentStatus = localStorage.getItem("is_Active");

        if (currentStatus === "true") {
            stopTimer();
            const userData = JSON.parse(localStorage.getItem('userData'));
            const newjwt = userData.jwt;


            const userName = localStorage.getItem('username');
            try {
                const response = await fetch('/webapi/myresource/newjwt', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        'jwt': newjwt
                    },
                    body: JSON.stringify({ username: userName })
                });

                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }

                const rowDetails = await response.json();
                const newJwt = rowDetails.jwt;
                console.log("New JWT: ", newJwt);

                userData.jwt = newJwt;
                localStorage.setItem('userData', JSON.stringify(userData));

                localStorage.setItem("is_Active", "false");
                restartTimer();

            } catch (error) {
                console.error('Error fetching row details:', error);
            }
        } else {
            console.log("Not Active");
        }
    };

    return (
        <MainContext.Provider value={selectedRow}>
            <div className="main_sec_container">
                <Filter
                    columnDisplayNames={displayNameMapping}
                    onFilter={handleFilter}
                    onSearch={handleSearch}
                    filterText={filterText}
                    filterColumn={filterParams.column}
                    filterOperator={filterParams.operator}
                    filterValue={filterParams.value}
                    data={data}
                    filteredData={filteredData}
                    rowsPerPage={rowsPerPage}
                    exportSpecificRows={exportSpecificRows}
                    generateCsvHeaders={generateCsvHeaders}
                    filterDataForExport={filterDataForExport}
                    userPre={userPre}
                    setUserPre={setUserPre}
                    setCurrentViewData={setCurrentViewData}
                    currentViewData={currentViewData}
                    columns={columns}
                />
                <Data_Table columns={columns} 
                filteredData={filteredData} 
                handleRowClicked={handleRowClicked} 
                rowsPerPage={rowsPerPage} 
                setRowsPerPage={setRowsPerPage} 
                getRowId={getRowId} 
                selectedRowId={selectedRowId} />
                <Tabs_Sec selectedRow={selectedRow} visibility ={Visibility} specVisibility={specVisibility} itemVisibility={itemVisibility} />
            </div>
        </MainContext.Provider>
    );
};

export default MainSection;
