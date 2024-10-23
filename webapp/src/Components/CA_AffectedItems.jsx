import React, { useState, useEffect, useRef } from "react";
import API_BASE_URL from "../config";
import DataTable from "react-data-table-component";
import icon from "../Icons/list.png";
import { Tabs_Sec_Popup } from "./Tabs_Sec_Popup";
import partIcon from "../Icons/PhysicalProduct.png";
import column from "../Icons/Columns.png";
import "../Styles/Tabs_Sec.css"; // Update based on the CSS file you need

export const Affected_popup = ({
  isOpen,
  onClose,
  rowData,
  supplierVisibilityValue,
  specVisibilityValue,
  itemVisibilityValue,
  selectedRData,
  partid,
  setSelectedRData,
}) => {
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
              setSelectedRData={setSelectedRData}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default function CA_AffectedItems({
  caId,
  acknowledgeValue,
  selectedRData,
  setSelectedRData,
  selectedName,
}) {
  const [tableData, setTableData] = useState([]);
  const [isPopupOpen, setIsPopupOpen] = useState(false);
  const [selectedRowData, setSelectedRowData] = useState(null);
  const [supplierVisibilityValue, setsupplierVisibilityValue] = useState(false);
  const [specVisibilityValue, setspecVisibilityValue] = useState(false);
  const [itemVisibilityValue, setItemVisibilityValue] = useState(false);
  const [collectedIds, setCollectedIds] = useState("");
  const [partid, setpartid] = useState(null);
  const [combinedAttributes, setCombinedAttributes] = useState([]);
  const [columns, setColumns] = useState([]);
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const dropdownRef = useRef(null); // To reference the dropdown for outside click detection

  useEffect(() => {
    const fetchAffectedItems = async () => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/SupplierPortal/getcaaffectedItems?caid=${caId}`
        );
        const data = await response.json();
        const objectArray = data.results || [];
        const formattedData = [];
        let supplierVisibility = null;
        let specVisibility = null;
        let itemVisibility = null;
        let allIds = [];
        if (objectArray.length > 0) {
          objectArray.forEach((objectDetails) => {
            const [key, details] = Object.entries(objectDetails)[0];

            if (details && details.attributes && details.basicAttributes) {
              const combinedAttribute = {};
              let partId = null;

              // Combine attributes
              const allAttributes = [
                ...details.basicAttributes,
                ...details.attributes,
              ];
              setCombinedAttributes(allAttributes); // Set combined attributes

              // Extract attributes
              allAttributes.forEach((attr) => {
                combinedAttribute[attr.displayName] = attr.value; // This will include dynamic attributes

                // Additional logic for static attributes, if needed
                if (attr.displayName === "Id") {
                  partId = attr.value;
                  allIds.push(partId);
                }
                // Set visibility values
                if (attr.displayName === "Supplier Visibility") {
                  supplierVisibility = attr.value === "ownedPart";
                }
                if (attr.displayName === "Supplier Item Visibility") {
                  specVisibility = attr.value !== "";
                }
                if (attr.displayName === "Supplier Spec Visibility") {
                  itemVisibility = attr.value !== "";
                }
              });

              // Set visibility values
              setsupplierVisibilityValue(supplierVisibility);
              setspecVisibilityValue(specVisibility);
              setItemVisibilityValue(itemVisibility);

              // Create formatted data object
              formattedData.push({
                ...combinedAttribute, // Spread combined attributes to include dynamic ones
                partId: partId,
              });
            }
          });
        }

        // Log formattedData to check extraction
        console.log("Formatted Data:", formattedData);
        setTableData(formattedData);
        const joinedIds = allIds.join("|");
        setCollectedIds(joinedIds);

        // Fetch Supplier Data
        const supplierData = await fetchSupplierData(joinedIds, caId);
        console.log("Fetched Supplier Data:", supplierData);

        // Extract supplier attributes
        const supplierAttributes =
          supplierData.results?.map((item) => {
            const key = Object.keys(item)[0];
            return { partId: key.split(": ")[1], ...item[key].attributes[0] };
          }) || [];

        // Filter formattedData based on supplierAttributes for each partId
        const filteredData = formattedData.filter((item) => {
          const supplierAttr = supplierAttributes.find(
            (attr) => attr.partId === item.partId
          );
          const isValid =
            supplierAttr &&
            (supplierAttr.supplier ||
              supplierAttr.supplieritem ||
              supplierAttr.supplierspec);
          return isValid;
        });

        console.log("Filtered Data:", filteredData);
        setTableData(filteredData);
      } catch (error) {
        console.error("Failed to fetch affected items:", error);
      }
    };
    const fetchSupplierData = async (collectedIds, caId) => {
      const url =
        `${API_BASE_URL}/SupplierPortal/getSupplierData`;
      const payload = {
        objectIds: collectedIds,
        caid: caId,
      };
      try {
        const response = await fetch(url, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
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
    // Close dropdown when clicking outside of it
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false); // Close the dropdown
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [caId, acknowledgeValue]);
  const handleActionClick = async (row) => {
    setSelectedRowData(row);
    setIsPopupOpen(true);
    try {
      const response = await fetch(
        `${API_BASE_URL}/SupplierPortal/parts?partid=${row.partId}`
      );
      if (!response.ok) {
        throw new Error(`Error: ${response.statusText}`);
      }
      const rowDetails = await response.json();
      setSelectedRData(rowDetails);
    } catch (error) {
      console.error("Failed to fetch row details:", error);
    }
  };

  // Log table data to check its structure
  console.log("Table Data:", tableData);
  // Define static columns
  const staticColumns = [
    {
      name: "Name",
      selector: (row) => row.Name,
      sortable: true,
      cell: (row) => (
        <div style={{ display: "flex", alignItems: "center" }}>
          <img
            src={partIcon}
            alt="PartIcon"
            style={{ width: "18px", height: "15px", marginRight: "3px" }}
          />
          <span>{row.Name}</span>
        </div>
      ),
    },
    {
      name: "Action",
      cell: (row) => (
        <img
          src={icon}
          alt="Action"
          className="Action"
          style={{ width: "25px", height: "22px", cursor: "pointer" }}
          title="Action"
          onClick={() => handleActionClick(row)}
        />
      ),
    },
    {
      name: "Revision",
      selector: (row) => row.Revision,
      sortable: true,
    },
    {
      name: "Description",
      selector: (row) => row.Description,
    },
    {
      name: "State",
      selector: (row) => row.State,
    },
    {
      name: "Owner",
      selector: (row) => row.Owner,
    },
  ];
  // Add only dynamic columns to the dropdown
  // Filter out "Supplier Item Visibility", "Supplier Visibility", and "ID" from dynamic columns
  const dynamicColumns = combinedAttributes.filter(
    (attr) =>
      ![
        "Supplier Item Visibility",
        "Supplier Visibility",
        "ID",
        "Name",
        "Owner",
        "Revision",
        "Id",
        "Description",
        "State",
      ].includes(attr.displayName)
  );

  const columnsDefinition = [
    {
      name: "Name",
      selector: (row) => row.Name,
      sortable: true,
      cell: (row) => (
        <div style={{ display: "flex", alignItems: "center" }}>
          <img
            src={partIcon}
            alt="PartIcon"
            style={{ width: "18px", height: "15px", marginRight: "3px" }}
          />
          <span>{row.Name}</span>
        </div>
      ),
    },
    {
      name: "Action",
      cell: (row) => (
        <img
          src={icon}
          alt="Action"
          className="Action"
          style={{ width: "25px", height: "22px", cursor: "pointer" }}
          title="Action"
          onClick={() => handleActionClick(row)}
        />
      ),
    },
    {
      name: "Revision",
      selector: (row) => row.Revision,
      sortable: true,
    },
    {
      name: "Description",
      selector: (row) => row.Description,
    },
    {
      name: "State",
      selector: (row) => row.State,
    },
    {
      name: "Owner",
      selector: (row) => row.Owner,
    },
  ];
  const handleSettingsClick = () => {
    // Logic to toggle dropdown visibility
    setDropdownOpen((prev) => !prev);
  };
  const handleColumnToggle = (columnName) => {
    const isSelected = columns.some((col) => col.name === columnName);
    if (isSelected) {
      setColumns((prev) => prev.filter((col) => col.name !== columnName));
    } else {
      const newColumn = {
        name: columnName,
        selector: (row) => row[columnName],
        sortable: true,
        cellClass: "selected-column", // Add class for styling
      };
      setColumns((prev) => [...prev, newColumn]);
    }
  };
  const isColumnSelected = (columnName) =>
    columns.some((col) => col.name === columnName);

  const finalColumns = [...staticColumns, ...columns];
  return (
    <div className="affecteditemsTable">
      <div className="header_right_settings">
        <button id="custom" onClick={handleSettingsClick}>
          <img src={column} alt="Customize Columns" />
        </button>
        {dropdownOpen && (
          <div className="dropdown-content" ref={dropdownRef}>
            <div className="scrollable-dropdown">
              {dynamicColumns.map((attr) => (
                <span
                  key={attr.name}
                  onClick={() => handleColumnToggle(attr.displayName)}
                  className={`dropdown-item ${
                    isColumnSelected(attr.displayName) ? "selected-column" : ""
                  }`}
                >
                  {attr.displayName}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {acknowledgeValue ? (
        <DataTable
          columns={finalColumns}
          data={tableData}
          pagination
          highlightOnHover
        />
      ) : (
        <div className="tableErrorMessage">
          <h4>
            Selected {selectedName} is not acknowledged. Please acknowledge the
            corresponding object in order to view the data.
          </h4>
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
