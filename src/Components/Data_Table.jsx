import { useEffect, useRef, useState } from 'react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-quartz.css';
import { AgGridReact } from 'ag-grid-react';
import "../Styles/Data_Table.css";

export const Data_Table = ({ filteredData, columns, handleRowClicked, rowsPerPage, setRowsPerPage, getRowId, selectedRowId }) => {

    const gridRef = useRef(); // Create a ref for AG-Grid instance
    const [rowCount, setRowCount] = useState(0); // State to store row count

    // Function to handle row count
    const getRowCount = () => {
        const count = gridRef.current?.api?.getDisplayedRowCount() || 0;
        setRowCount(count); // Store row count in state
    };

    // Call getRowCount whenever the data changes
    useEffect(() => {
        if (gridRef.current) getRowCount();
    }, [filteredData]);

    const handlePerPageChange = (newRowsPerPage) => {
        setRowsPerPage(newRowsPerPage);
    };

    const paginationPageSizeSelector = [6, 10, 12];

    const getRowClass = (params) => {
        return params.data.objectId === selectedRowId ? 'highlight' : '';
    };

    return (
        <div className="ag-theme-quartz" style={{ height: 450, marginTop: '0%' }}>
            <AgGridReact
                ref={gridRef} // Attach the ref to AG-Grid
                rowData={filteredData}
                columnDefs={columns}
                pagination={true}
                paginationPageSize={rowsPerPage}
                onPaginationChanged={params => handlePerPageChange(params.api.paginationGetPageSize())}
                paginationPageSizeSelector={paginationPageSizeSelector}
                rowHeight={55}
                tooltipShowDelay={500}
                onRowClicked={handleRowClicked}
                getRowId={getRowId}
                getRowClass={getRowClass}
            />
        </div>
    );
};
