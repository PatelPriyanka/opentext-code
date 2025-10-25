import React from 'react';
import PartnerCard from './PartnerCard';

function PartnerGrid({ partners }) {
    return (
        // Responsive grid layout
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {partners.map(partner => (
                <PartnerCard key={partner.partnerId || partner.partnerName} partner={partner} />
            ))}
        </div>
    );
}

export default PartnerGrid;
