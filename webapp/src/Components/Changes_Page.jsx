import React, { createContext, useState, useEffect, useRef } from "react";
import API_BASE_URL from "../config";
import { AgGridReact } from "ag-grid-react";
import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-quartz.css";
import acknowledgeIcon from "../Icons/Ack.png";
import nonAcknowledgeIcon from "../Icons/NotAck.png";
import "../Styles/Nav.css";
import changeActionImage from "../Icons/ChangeAction.png";
import { Tabs, Tab, TabList, TabPanel } from "react-tabs";
import jsonStructure from "./columnOrderConfig.json";
import CA_AffectedItems from "./CA_AffectedItems";
import { Tabs_Sec } from "./Tabs_Sec";
import loadingIcon from "../Icons/loading.png"; // Import the loading icon

const MainContext = createContext();
// Modal Component
const Modal = ({ isOpen, onClose, children }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <button onClick={onClose} className="close-button">
          X
        </button>
        {children}
      </div>
    </div>
  );
};

export const Changes_Page = ({
  selectedRData,
  data,
  setSelectedRData,
  caObjectIds,
}) => {
  const [acknowledged, setAcknowledgeValue] = useState(false);
  const [tableData, setTableData] = useState([]);
  const [selectedTab, setSelectedTab] = useState(0);
  const [selectedRowId, setSelectedRowId] = useState(null);
  const [caCount, setCaCount] = useState(0);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRow, setSelectedRow] = useState(null);
  const [acknowledgeState, setAcknowledgeState] = useState(null);
  const [selectedDetails, setSelectedDetails] = useState(null);
  const username = localStorage.getItem("username");
  const [selectedName, setSelectName] = useState(null);
  const [caId, setCaId] = useState(null);
  const [acknowledgeValue, setAcknowledgedValue] = useState(false);
  const [filteredData, setFilteredData] = useState([]);
  const [isGeneralOpen, setIsGeneralOpen] = useState(true);
  const [loading, setLoading] = useState(true);
  const [processDetails, setProcessDetails] = useState({
    changeActionName: "",
  });

  // Create a ref for the grid
  const gridRef = useRef(null);

  const toggleGeneral = () => setIsGeneralOpen(!isGeneralOpen);

  useEffect(() => {
    // Show loading spinner initially
    setLoading(true);
    const timer = setTimeout(() => {
      setLoading(false);
    }, 1000);

    return () => clearTimeout(timer);
  }, []);

  const [columnDefs] = useState([
    {
      field: "Name",
      headerName: "Name",
      sortable: true,
      filter: true,
      width: "400px",
    },
    {
      field: "Description",
      headerName: "Description",
      sortable: true,
      filter: true,
      width: "565px",
    },
    {
      field: "Owner",
      headerName: "Owner",
      sortable: true,
      filter: true,
      width: "350px",
    },
    {
      field: "Action",
      headerName: "Action",
      filter: true,
      cellRenderer: (params) => {
        return params.data.acknowledged ? (
          <img
            src={acknowledgeIcon}
            alt="AcknowledgeIcon"
            className="AcknowledgeIcon"
            style={{
              width: "29px",
              height: "28px",
              cursor: "pointer",
              marginLeft: "7.5px",
            }}
            title="Acknowledge"
            onClick={() => handleIconClick(params.data, true)}
          />
        ) : (
          <img
            src={nonAcknowledgeIcon}
            alt="nonAcknowledgeIcon"
            className="nonAcknowledgeIcon"
            style={{
              width: "29px",
              height: "28px",
              cursor: "pointer",
              marginLeft: "7.5px",
            }}
            title="Non-Acknowledged"
            onClick={() => handleIconClick(params.data, false)}
          />
        );
      },
      width: "200px",
      minWidth: 100,
      maxWidth: 300,
    },
  ]);

  useEffect(() => {
    fetch(
      `${API_BASE_URL}/SupplierPortal/getSupplierDetailsByEmail?email=${username}`
    )
      .then((res) => res.json())
      .then((data) => {
        const objectArray = data.results || [];
        const newTableData = [];
        let changeActionCount = 0;

        if (objectArray.length > 0) {
          objectArray.forEach((objectDetails) => {
            const [key, details] = Object.entries(objectDetails)[0];

            if (details) {
              const combinedAttribute = {};
              let CAID = "";
              let acknowledgeValue = false;

              details.attributes?.forEach((attr) => {
                combinedAttribute[attr.displayName] = attr.value;
                if (attr.displayName === "CAID") {
                  CAID = attr.value;
                }
                if (attr.displayName === "Acknowledge") {
                  acknowledgeValue = attr.value === "Yes";
                }
              });

              if (caObjectIds.length === 0 || caObjectIds.includes(CAID)) {
                newTableData.push({
                  Name: combinedAttribute["Name"] || "",
                  Description: combinedAttribute["Description"] || "",
                  Owner: combinedAttribute["Owner"] || "",
                  acknowledged: acknowledgeValue,
                  objectId: CAID,
                  extraData: details,
                });
                changeActionCount++;
              }
            }
          });

          setTableData(newTableData);
          setCaCount(changeActionCount);
        }
      })
      .catch((error) => console.error("Error fetching data:", error));
  }, [username, caObjectIds]);

  const handleGridReady = (params) => {
    if (tableData.length > 0) {
      const firstNode = params.api.getRowNode(
        params.api.getDisplayedRowAtIndex(0).id
      );
      if (firstNode) {
        firstNode.setSelected(true); // Select the first row
        handleRowClicked({ data: firstNode.data }); // Optionally trigger row click handler
      }
    }
  };

  const handleIconClick = (row, isAcknowledged) => {
    setSelectedRow(row);
    setAcknowledgeState(isAcknowledged);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedRow(null);
    setAcknowledgeState(null);
  };

  const handleSetData = async () => {
    if (selectedRow) {
      const value = acknowledged;
      if (username) {
        const response = await updateAcknowledgmentStatus(
          selectedRow.objectId,
          value,
          username
        );
        if (response.ok) {
          const updatedTableData = tableData.map((row) => {
            if (row.objectId === selectedRow.objectId) {
              return { ...row, acknowledged: value };
            }
            return row;
          });
          setTableData(updatedTableData);
          console.log("Acknowledgment status updated successfully");
        } else {
          console.error("Failed to update acknowledgment status");
        }
      } else {
        console.error("Username not found in local storage");
      }
    }

    setIsModalOpen(false);
    setSelectedRow(null);
    setAcknowledgeState(null);
  };

  const handleRadioChange = (e) => {
    setAcknowledgeValue(e.target.value === "Yes");
  };

  const handleRowClicked = async (event) => {
    const objectId = event.data.objectId;
    setSelectedRowId(objectId);
    const name = event.data.Name;
    setSelectName(name);
    const acknowledge = event.data.acknowledged;
    setCaId(objectId);
    setAcknowledgedValue(acknowledge);
    console.log("Clicked row objectId:", objectId);
    const details = await fetchChangeActionDetails(objectId);

    setSelectedDetails({
      ...details,
      acknowledged: event.data.acknowledged,
    });
    setSelectedTab(0);
  };

  const getRowClass = (params) => {
    return params.data.objectId === selectedRowId ? "highlight" : "";
  };

  const updateAcknowledgmentStatus = async (objectId, value, username) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/SupplierPortal/updateAcknowledgedInfo`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            objectId: objectId,
            value: `${value}`,
            username: username,
          }),
        }
      );

      if (!response.ok) {
        throw new Error("Network response was not ok");
      }

      return response;
    } catch (error) {
      console.error("Error updating acknowledgment status:", error);
      return {};
    }
  };

  const fetchChangeActionDetails = async (objectId) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/SupplierPortal/getchangeactiondetails?caid=${objectId}`
      );

      if (!response.ok) {
        throw new Error("Network response was not ok");
      }

      const data = await response.json();
      const objectData = data.results || [];
      const combinedAttribute = {};

      if (objectData.length > 0) {
        objectData.forEach((objectDataDetails) => {
          const [key, details] = Object.entries(objectDataDetails)[0];

          if (details) {
            details.attributes?.forEach((attr) => {
              combinedAttribute[attr.displayName] = attr.value;
            });
          } else {
            console.error("No Object found:", key);
          }
        });

        return combinedAttribute;
      } else {
        console.error("No results found");
        return {};
      }
    } catch (error) {
      console.error("Error fetching data:", error);
      return {};
    }
  };

  const toggleGroup = (groupName) => {
    setExpandedGroups((prevState) => ({
      ...prevState,
      [groupName]: !prevState[groupName], // Toggle the specific group's state
    }));
  };
  const paginationPageSizeSelector = [6, 10, 12];
  const fieldGroups = jsonStructure.ChangeAction;
  const [expandedGroups, setExpandedGroups] = useState(
    // Initialize every group to be expanded by default
    Object.keys(fieldGroups).reduce((acc, groupName) => {
      acc[groupName] = true; // All groups are open by default
      return acc;
    }, {})
  );

  const renderGroupedFields = (details) => {
    if (!details) return null;

    return Object.entries(fieldGroups).map(([groupName, fields]) => (
      <div key={groupName} className="group">
        <div className="groupHeader" onClick={() => toggleGroup(groupName)}>
          <h4 className="groupHeader-title">{groupName}</h4>
          <i
            className={`ri-arrow-down-double-line ${
              expandedGroups[groupName] ? "rotate_icon" : ""
            }`}
          ></i>
        </div>
        <div
          className={`details-grid ${expandedGroups[groupName] ? "open" : ""}`}
        >
          {fields.map((field) => (
            <div className="details-grid-row" key={field}>
              <div className="details-label">
                {field}:
                <span className="details-value">{details[field] || "N/A"}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    ));
  };

  const onGridReady = (params) => {
    if (tableData.length > 0) {
      const firstNode = params.api.getRowNode(
        params.api.getDisplayedRowAtIndex(0).id
      );
      if (firstNode) {
        firstNode.setSelected(true); // Select the first row
        setSelectedRowId(firstNode.data.objectId); // Set the selectedRowId
        handleRowClicked({ data: firstNode.data }); // Optionally trigger row click handler
      }
    }
  };
  const onRowClicked = (row) => {
    setSelectedRowId(row.data.objectId); // Update selectedRowId on row click
  };

  return (
    <MainContext.Provider value={data}>
      <div>
        {loading ? (
          <div className="loading-container">
            <img
              src={loadingIcon}
              alt="Loading"
              className="changesloading-logo"
            />
          </div>
        ) : (
          <div>
            <div
              className="ag-theme-quartz"
              style={{
                height: "100%",
                width: "100%",
                marginTop: "100px",
              }}
            >
              <AgGridReact
                rowData={tableData}
                columnDefs={columnDefs}
                pagination={true}
                paginationPageSize={6}
                paginationPageSizeSelector={paginationPageSizeSelector}
                onRowClicked={handleRowClicked}
                getRowClass={getRowClass}
                suppressRowClickSelection={true}
                selectedRowId={selectedRowId}
                rowSelection="none"
                domLayout="autoHeight"
                getRowId={(params) => params.data.objectId}
                defaultColDef={{
                  resizable: true,
                }}
                onGridReady={onGridReady}
              />
            </div>
            <Tabs
              selectedIndex={selectedTab}
              onSelect={(index) => setSelectedTab(index)}
              selectedRow={data}
            >
              <TabList>
                <Tab
                  className={
                    selectedTab === 0
                      ? "selectedTabColourHandel"
                      : "nonselectedtabGrayOut"
                  }
                >
                  <div className="ca-tab-content">
                    <img
                      src={changeActionImage}
                      className="tab-icon"
                      alt="Change Action"
                    />
                    <div className="tab-content-size">
                      ChangeAction Detail(s)
                    </div>
                  </div>
                </Tab>
                <Tab
                  className={
                    selectedTab === 1
                      ? "selectedTabColourHandel"
                      : "nonselectedtabGrayOut"
                  }
                >
                  <div className="ca-tab-content">
                    <img
                      src={changeActionImage}
                      className="tab-icon"
                      alt="Affected Items"
                    />
                    <div className="tab-content-size">Affected Item(s)</div>
                  </div>
                </Tab>
              </TabList>
              <TabPanel>
                {selectedDetails && (
                  <div className="change-action-details">
                    {selectedDetails.acknowledged ? (
                      <div className="caDetails" onClick={toggleGeneral}>
                        {renderGroupedFields(selectedDetails)}
                      </div>
                    ) : (
                      <div className="caError">
                        <h4>
                          Selected {selectedName} is not acknowledged. Please
                          acknowledge the corresponding object in order to view
                          the data.
                        </h4>
                      </div>
                    )}
                  </div>
                )}
              </TabPanel>
              <TabPanel>
                <CA_AffectedItems
                  caId={caId}
                  acknowledgeValue={acknowledgeValue}
                  selectedRData={selectedRData}
                  setSelectedRData={setSelectedRData}
                  selectedName={selectedName}
                  selectedRow={data}
                />
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
          </div>
        )}
      </div>
    </MainContext.Provider>
  );
};
