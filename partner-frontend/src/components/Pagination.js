import React from 'react';

function Pagination({ currentPage, totalPages, onNext, onPrev }) {
    return (
        <div className="flex justify-between items-center mt-10">
            <button
                onClick={onPrev}
                disabled={currentPage === 0}
                className="bg-white border border-ot-gray-border text-ot-text-dark font-semibold px-5 py-2 rounded-md transition-colors duration-200 hover:bg-gray-50 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed"
            >
                Previous
            </button>
            
            <span className="text-sm font-medium text-ot-text-light">
                Page {currentPage + 1} of {totalPages}
            </span>
            
            <button
                onClick={onNext}
                disabled={currentPage >= totalPages - 1}
                className="bg-ot-blue border border-ot-blue text-white font-semibold px-5 py-2 rounded-md transition-colors duration-200 hover:bg-ot-blue-dark disabled:bg-gray-300 disabled:border-gray-300 disabled:cursor-not-allowed"
            >
                Next
            </button>
        </div>
    );
}

export default Pagination;
