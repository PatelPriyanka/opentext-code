import React from 'react';

function Header({ hasSolutions, onFilterChange }) {
    return (
        <header className="flex flex-col md:flex-row justify-between md:items-center mb-6 md:mb-8 border-b border-ot-gray-border pb-6">
            <h1 className="text-3xl font-bold text-ot-blue mb-4 md:mb-0">
                Partner Directory
            </h1>
            <div className="flex items-center bg-white p-4 rounded-lg shadow-sm border border-ot-gray-border">
                <input
                    type="checkbox"
                    id="has-solutions-filter"
                    className="ot-checkbox"
                    checked={hasSolutions}
                    onChange={onFilterChange}
                />
                <label htmlFor="has-solutions-filter" className="ml-3 font-medium text-sm text-ot-text-light cursor-pointer select-none">
                    Only show partners with listed solutions
                </label>
            </div>
        </header>
    );
}

export default Header;
