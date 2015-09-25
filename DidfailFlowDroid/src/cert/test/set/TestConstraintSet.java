package cert.test.set;

import org.junit.Assert;
import org.junit.Test;

import cert.didfail.flowdroid.ActionStringConstraintSet;

public class TestConstraintSet {

	@Test
	public void testDefault() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		Assert.assertTrue(set.contains("action1"));
		Assert.assertTrue(set.contains("action2"));
		Assert.assertTrue(set.contains("action3"));
		Assert.assertFalse(set.isEmpty());
		Assert.assertTrue(set.isUniverse());
	}

	@Test
	public void testAnd() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", false);
		Assert.assertTrue(set.contains("action1"));
		Assert.assertFalse(set.contains("action2"));
		Assert.assertFalse(set.contains("action3"));
		Assert.assertFalse(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testMultipleAnd() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", false);
		set.and("action2", false);
		Assert.assertFalse(set.contains("action1"));
		Assert.assertFalse(set.contains("action2"));
		Assert.assertFalse(set.contains("action3"));
		Assert.assertTrue(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testAndWithNegate() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", true);
		Assert.assertFalse(set.contains("action1"));
		Assert.assertTrue(set.contains("action2"));
		Assert.assertTrue(set.contains("action3"));
		Assert.assertFalse(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testAndWithMultipleNegates() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", true);
		set.and("action2", true);
		set.and("action3", true);

		Assert.assertFalse(set.contains("action1"));
		Assert.assertFalse(set.contains("action2"));
		Assert.assertFalse(set.contains("action3"));
		Assert.assertTrue(set.contains("action4"));
		Assert.assertTrue(set.contains("action5"));
		Assert.assertTrue(set.contains("action6"));
		Assert.assertFalse(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testAndMix1() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", false);
		set.and("action2", true);

		Assert.assertTrue(set.contains("action1"));
		Assert.assertFalse(set.contains("action2"));
		Assert.assertFalse(set.contains("action3"));
		Assert.assertFalse(set.contains("action4"));
		Assert.assertFalse(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testAndMix2() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", true);
		set.and("action2", true);
		set.and("action3", true);
		set.and("action4", true);
		set.and("action5", false);

		Assert.assertTrue(set.contains("action5"));
		Assert.assertFalse(set.contains("action1"));
		Assert.assertFalse(set.contains("action2"));
		Assert.assertFalse(set.contains("action3"));
		Assert.assertFalse(set.contains("action4"));

		Assert.assertFalse(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testAndMix3() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", true);
		set.and("action1", false);

		Assert.assertFalse(set.contains("action1"));
		Assert.assertFalse(set.contains("action2"));
		Assert.assertFalse(set.contains("action3"));

		Assert.assertTrue(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testAndMix4() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", true);
		set.and("action1", false);
		set.and("action2", false);
		set.and("action3", false);
		set.and("action4", false);
		set.and("action5", false);
		set.and("action6", true);
		set.and("action7", true);
		set.and("action8", true);
		set.and("action9", true);

		Assert.assertFalse(set.contains("action1"));
		Assert.assertFalse(set.contains("action2"));
		Assert.assertFalse(set.contains("action3"));
		Assert.assertFalse(set.contains("action4"));
		Assert.assertFalse(set.contains("action5"));
		Assert.assertFalse(set.contains("action6"));
		Assert.assertFalse(set.contains("action7"));
		Assert.assertFalse(set.contains("action8"));
		Assert.assertFalse(set.contains("action9"));

		Assert.assertTrue(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testOr1() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.or("action1", false);
		Assert.assertTrue(set.contains("action1"));
		Assert.assertTrue(set.contains("action2"));
		Assert.assertTrue(set.contains("action3"));
		Assert.assertFalse(set.isEmpty());
		Assert.assertTrue(set.isUniverse());
	}

	@Test
	public void testOr2() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.or("action1", true);
		Assert.assertTrue(set.contains("action1"));
		Assert.assertTrue(set.contains("action2"));
		Assert.assertTrue(set.contains("action3"));
		Assert.assertFalse(set.isEmpty());
		Assert.assertTrue(set.isUniverse());
	}

	@Test
	public void testOr3() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", false);
		set.or("action2", false);

		Assert.assertTrue(set.contains("action1"));
		Assert.assertTrue(set.contains("action2"));
		Assert.assertFalse(set.contains("action3"));
		Assert.assertFalse(set.contains("action4"));
		Assert.assertFalse(set.contains("action5"));
		Assert.assertFalse(set.contains("action6"));

		Assert.assertFalse(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}

	@Test
	public void testOr4() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", false);
		set.or("action1", true);

		Assert.assertTrue(set.contains("action1"));
		Assert.assertTrue(set.contains("action2"));
		Assert.assertTrue(set.contains("action3"));
		Assert.assertTrue(set.contains("action4"));
		Assert.assertTrue(set.contains("action5"));
		Assert.assertTrue(set.contains("action6"));

		Assert.assertFalse(set.isEmpty());
		Assert.assertTrue(set.isUniverse());
	}
	
	@Test
	public void testOr5() {
		ActionStringConstraintSet set = new ActionStringConstraintSet();
		set.and("action1", false);
		set.or("action2", true);

		Assert.assertTrue(set.contains("action1"));
		Assert.assertFalse(set.contains("action2"));
		Assert.assertTrue(set.contains("action3"));
		Assert.assertTrue(set.contains("action4"));
		Assert.assertTrue(set.contains("action5"));
		Assert.assertTrue(set.contains("action6"));

		Assert.assertFalse(set.isEmpty());
		Assert.assertFalse(set.isUniverse());
	}
}
