import React from 'react';
import SolutionList from './SolutionList';

// Helper function to remove basic HTML tags from descriptions
const cleanDescription = (html) => {
    if (!html) return 'No description available.';
    // Use DOMParser to safely extract text content
    const doc = new DOMParser().parseFromString(html, 'text/html');
    return (doc.body.textContent || "")
        .replace(/&nbsp;/g, ' ')
        .trim();
}

function PartnerCard({ partner }) {
    const description = cleanDescription(partner.shortDescription || partner.companyOverview);

    return (
        <div className="bg-white border border-ot-gray-border rounded-lg shadow-md p-6 flex flex-col h-full transition-all duration-300 hover:shadow-xl">
            <h2 className="text-xl font-bold text-ot-blue mb-2">
                {partner.partnerName || 'Unnamed Partner'}
            </h2>
            
            {/* Partner Level and Type Tags */}
            <div className="flex flex-wrap gap-2 mb-4">
                {partner.partnerLevel && (
                    <span className="text-xs font-semibold bg-gray-100 text-gray-700 px-3 py-1 rounded-full">
                        {partner.partnerLevel}
                    </span>
                )}
                {partner.partnerType && (
                    <span className="text-xs font-semibold bg-gray-100 text-gray-700 px-3 py-1 rounded-full">
                        {partner.partnerType}
                    </span>
                )}
            </div>

            {/* Description - flex-grow ensures all cards in a row are the same height */}
            <p className="text-sm text-ot-text-light mb-5 flex-grow">
                {description}
            </p>

            <SolutionList solutions={partner.solutions} />
        </div>
    );
}

export default PartnerCard;
