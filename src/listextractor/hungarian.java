package listextractor;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.Math;

public class hungarian{
	double cost[][];
	int n, max_match;
	double lx[], ly[];
	int xy[];
	int yx[];
	boolean S[], T[];
	double slack[];
	int slackx[];
	int prev[];
	public hungarian(double ec[][],int num){
		n=num;
	    cost = ec;
		lx = new double[n];
		ly = new double[n];
		xy = new int[n];
		yx = new int[n];
		S = new boolean[n];
		T = new boolean[n];
		slack = new double[n];
		slackx = new int[n];
		prev = new int[n];
		Arrays.fill(xy,-1);
	    Arrays.fill(yx,-1);
	}
	void init_labels()
	{
	    Arrays.fill(lx,0);
	    Arrays.fill(ly,0);
	    for (int x = 0; x < n; x++)
	        for (int y = 0; y < n; y++)
	            lx[x] = Math.max(lx[x], cost[x][y]);
	}
	void update_labels()
	{
	    int x, y;
	    double delta = 10;
	    for (y = 0; y < n; y++)
	        if (!T[y])
	            delta = Math.min(delta, slack[y]);
	    for (x = 0; x < n; x++)
	        if (S[x]) lx[x] -= delta;
	    for (y = 0; y < n; y++)
	        if (T[y]) ly[y] += delta;
	    for (y = 0; y < n; y++)
	        if (!T[y])
	            slack[y] -= delta;
	}
	void add_to_tree(int x, int prevx) 
	{
	    S[x] = true;
	    prev[x] = prevx;               
	    for (int y = 0; y < n; y++) 
	        if (lx[x] + ly[y] - cost[x][y] < slack[y])
	        {
	            slack[y] = lx[x] + ly[y] - cost[x][y];
	            slackx[y] = x;
	        }
	}
	void augment()                         
	{
	    if (max_match == n) return;
	    int x, y, root=-1;
	    int q[] = new int[n], wr = 0, rd = 0;
	    Arrays.fill(S,false);
	    Arrays.fill(T,false);
	    Arrays.fill(prev,-1);
	    for (x = 0; x < n; x++)
	        if (xy[x] == -1)
	        {
	            q[wr++] = root = x;
	            prev[x] = -2;
	            S[x] = true;
	            break;
	        }
	    for (y = 0; y < n; y++)
	    {
	        slack[y] = lx[root] + ly[y] - cost[root][y];
	        slackx[y] = root;
	    }
	    while (true)
	    {
	        while (rd < wr)
	        {
	            x = q[rd++];
	            for (y = 0; y < n; y++)
	                if (cost[x][y] == lx[x] + ly[y] &&  !T[y])
	                {
	                    if (yx[y] == -1) break;
	                    T[y] = true;
	                    q[wr++] = yx[y];
	                    add_to_tree(yx[y], x);
	                }
	            if (y < n) break;
	        }
	        if (y < n) break;
	        update_labels();
	        wr = rd = 0;                
	        for (y = 0; y < n; y++)        
	            if (!T[y] &&  slack[y] == 0)
	            {
	                if (yx[y] == -1)
	                {
	                    x = slackx[y];
	                    break;
	                }
	                else
	                {
	                    T[y] = true;
	                    if (!S[yx[y]])    
	                    {
	                        q[wr++] = yx[y];
	                        add_to_tree(yx[y], slackx[y]);
	                    }
	                }
	            }
	        if (y < n) break;
	    }

	    if (y < n)
	    {
	        max_match++;
	        for (int cx = x, cy = y, ty; cx != -2; cx = prev[cx], cy = ty)
	        {
	            ty = xy[cx];
	            yx[cy] = cx;
	            xy[cx] = cy;
	        }
	        augment();
	    }
	}
	void solve()
	{
	    max_match = 0;
	    init_labels();
	    augment();
	}
	double match(){
		double ret = 0;
		for (int x = 0; x < n; x++)
	        ret += cost[x][xy[x]];
	    return ret;
	}
	double mcount(){
		double ret = 0;
		for (int x = 0; x < n; x++)
	        if(cost[x][xy[x]]!=0) ret++;
	    return ret;
	}
	double ymatch(){
		double ret = 0;
		for (int y = 0; y < n; y++)
	        ret += cost[y][yx[y]];
	    return ret;
	}
	double ymcount(){
		double ret = 0;
		for (int y = 0; y < n; y++)
	        if(cost[y][yx[y]]!=0) ret++;
	    return ret;
	}
	int[] assign(){
		return xy;
	}
	public static void main(String args[]){
		double c[][] = new double[2][2];
		c[0][0] = 2; c[1][1] = 1;
		c[1][0] = 0; c[0][1] = 2;
		hungarian h = new hungarian(c,2);
		h.solve();
		System.out.println(h.match()+": "+h.assign()[0]+" "+h.assign()[1]);
	}
}