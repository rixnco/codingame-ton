package test;

import static org.junit.Assert.*;

import org.junit.Test;

import tron.*;
import tron.Grid.Direction;

public class GridTest {

	@Test
	public void testUnmove() {
		
		Grid g0= new Grid(2);
		
		
		Grid g= g0.copy();
		
		// First move
		assertTrue(g.move(0, Grid.getXY(11, 11)));
	
			Grid g1= g.copy();
			// Second move
			assertTrue(g.move(0, Direction.UP));
			
				Grid g2= g.copy();
				// Suicide
				assertFalse(g.move(0,Direction.DOWN));
				assertArrayEquals(g0.grid, g.grid);
				
					// NOP
					assertFalse(g.move(0,Direction.LEFT));
					assertArrayEquals(g0.grid, g.grid);
				
				// Resurect
				assertTrue(g.unmove());
				assertArrayEquals(g2.grid, g.grid);
	
			
			assertTrue(g.unmove());
			assertArrayEquals(g1.grid, g.grid);
		
		assertTrue(g.unmove());
		assertArrayEquals(g0.grid, g.grid);
		
		// NOP
		assertFalse(g.unmove());
	}

}
