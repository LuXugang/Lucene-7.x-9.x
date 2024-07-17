package org.example;

public class TestIsland {
    // 深度遍历
    public static void dfs(char[][] grid, boolean[][] visited, int r, int c, int row, int cols) {
        // 退出
        if (r < 0 || r >= row || c < 0 || grid[r][c] == '0' || visited[r][c] || c >= cols) {
            return;
        }
        //记录访问
        visited[r][c] = true;
        dfs(grid, visited, r - 1, c, row, cols);
        dfs(grid, visited, r + 1, c, row, cols);
        dfs(grid, visited, r, c - 1, row, cols);
        dfs(grid, visited, r, c + 1, row, cols);
    }

    public static int islandsNum(char[][] grid) {
        if (grid == null || grid.length == 0) {
            return 0;
        }

        int islandNum = 0;
        // 长度
        int row = grid.length;
        int cols = grid[0].length;

        // 访问过
        boolean[][] visited = new boolean[row][cols];

        // 遍历row
        for (int r = 0; r < row; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == '1' && !visited[r][c]) {
                    islandNum++;
                    // 深度遍历
                    dfs(grid, visited, r, c, row, cols);
                }
            }
        }

        return islandNum;
    }

    public static void main(String[] args) {
        char[][] grid = {

                {'1', '1', '1', '1', '0'},

                {'1', '1', '0', '1', '0'},

                {'1', '1', '0', '0', '0'},

                {'0', '0', '0', '0', '0'}};

        System.out.println(islandsNum(grid));
        char[][] grid1 = {

                {'1', '1', '0', '0', '0'},

                {'1', '1', '0', '0', '0'},

                {'0', '0', '1', '0', '0'},

                {'0', '0', '0', '1', '1'}


        };
        System.out.println(islandsNum(grid1));
    }
}
