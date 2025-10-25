import React from 'react';

function StatusMessage({ message }) {
    return (
        <div className="text-center text-lg text-ot-text-light p-12 bg-white rounded-lg border border-ot-gray-border shadow-sm">
            {message}
        </div>
    );
}

export default StatusMessage;
