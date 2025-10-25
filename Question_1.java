class Main {

    // 1. Display a triangle of 3x4 (Height=3, Base=4)
    public static void display3x4Triangle() {
        displayMxNTriangle(3,4);
    } 
	//Display MxN Triangle
    public static void displayMxNTriangle(int m, int n) {
        
        if (m <= 0 || n <= 0) {
            System.out.println("Error: M and N must be positive integers.");
            return;
        }

        for (int i = 1; i <= m; i++) {
            
            int stars = (int) Math.round((double) i * n / m);
            if(i==1 && m>1){
                stars = 1;
            }
            
            stars = Math.max(1, Math.min(n, stars));
            System.out.println("*".repeat(stars));
        }
    }
    
    public static void main(String[] args) {
        System.out.println("--- 3x4 Triangle ---");
        display3x4Triangle();
        
        System.out.println("\n--- MxN Triangle (Bonus) M=5, N=10 ---");
        displayMxNTriangle(5, 10); 

    }

}
// Assumption: The triangle is a right-angled triangle.
// M (Height) = 3: Number of rows
// N (Base) = 4: Number of characters in the final (M-th) row
// Logic: For each row 'i' from 1 to 'height', print 'stars' characters.
// The number of characters 'stars' is calculated by linear interpolation:
// When i=1, stars should be (1 * base) / height = 4/3 ~ 1
// When i=2, stars should be (2 * base) / height = 8/3 ~ 2

// When i=3, stars should be (3 * base) / height = 4
