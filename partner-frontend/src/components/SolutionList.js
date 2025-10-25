import React from 'react';

function SolutionList({ solutions }) {
    if (!solutions || solutions.length === 0) {
        return (
            <div className='mt-4'>
                <h3 className="text-sm font-semibold text-gray-400 mb-2">SOLUTIONS</h3>
                <p className="text-sm italic text-gray-400">No listed solutions.</p>
            </div>
        );
    }

    return (
        <div className="border-t border-ot-gray-border pt-4 mt-auto">
            <h3 className="text-sm font-semibold text-gray-500 mb-3">SOLUTIONS</h3>
            <ul className="space-y-3">
                {solutions.map((solution, index) => (
                    <li key={index} className="text-sm">
                        <strong className="text-gray-700 block">
                            {solution.displayName || 'N/A'}
                        </strong>
                        <p className="text-gray-500 line-clamp-2">
                            {solution.shortDescription || ''}
                        </p>
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default SolutionList;
