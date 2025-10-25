import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import PartnerGrid from './components/PartnerGrid';
import Pagination from './components/Pagination';
import LoadingSpinner from './components/LoadingSpinner';
import StatusMessage from './components/StatusMessage';

// Configuration constants
const PAGE_SIZE = 12;
// In development, React runs on 3000 and calls Spring Boot on 8080
const API_URL = 'http://localhost:8080/api/partners'; 

function App() {
    const [partners, setPartners] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [hasSolutions, setHasSolutions] = useState(false);

    // Effect to fetch data when page, size, or filter changes
    useEffect(() => {
        const fetchPartners = async () => {
            setIsLoading(true);
            setError(null);
            
            try {
                // Construct query parameters
                const params = new URLSearchParams({
                    page: currentPage,
                    size: PAGE_SIZE,
                    hasSolutions: hasSolutions,
                });
                
                const response = await fetch(`${API_URL}?${params.toString()}`);
                
                if (!response.ok) {
                    // Check for network or server-side error
                    throw new Error(`Failed to fetch partners. Status: ${response.status}`);
                }
                
                const data = await response.json();
                
                // Update state with fetched data
                setPartners(data.content || []);
                setTotalPages(data.totalPages || 0);
                setCurrentPage(data.number || 0);

            } catch (err) {
                // Catch any fetch or parsing errors
                setError(`API Error: ${err.message}. Make sure the Spring Boot backend is running on port 8080.`);
                setPartners([]);
            } finally {
                setIsLoading(false);
            }
        };

        fetchPartners();
    }, [currentPage, hasSolutions]); // Dependencies

    // Handler for filter checkbox change
    const handleFilterChange = (e) => {
        setHasSolutions(e.target.checked);
        setCurrentPage(0); // Reset to first page when filter changes
    };
    
    // Pagination handlers
    const handleNextPage = () => {
        if (currentPage < totalPages - 1) {
            setCurrentPage(currentPage + 1);
        }
    };

    const handlePrevPage = () => {
        if (currentPage > 0) {
            setCurrentPage(currentPage - 1);
        }
    };
    
    // Centralized function to determine what content to render
    const renderContent = () => {
        if (isLoading) {
            return <LoadingSpinner />;
        }
        if (error) {
            return <StatusMessage message={error} />;
        }
        if (partners.length === 0) {
            return <StatusMessage message="No partners found matching your criteria." />;
        }
        return <PartnerGrid partners={partners} />;
    };

    return (
        <div className="max-w-7xl mx-auto p-4 md:p-8">
            <Header 
                hasSolutions={hasSolutions} 
                onFilterChange={handleFilterChange} 
            />
            
            <main>
                {renderContent()}
                
                {/* Only show pagination if not loading/errored and multiple pages exist */}
                {!isLoading && !error && totalPages > 1 && (
                    <Pagination 
                        currentPage={currentPage}
                        totalPages={totalPages}
                        onNext={handleNextPage}
                        onPrev={handlePrevPage}
                    />
                )}
            </main>
        </div>
    );
}

export default App;
