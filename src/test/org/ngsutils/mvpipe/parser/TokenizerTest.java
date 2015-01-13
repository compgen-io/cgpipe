package org.ngsutils.mvpipe.parser;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TokenizerTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTokenize() {
		assertListEquals(listBuild("a", "=", "b"), Tokenizer.tokenize("a = b"));
		assertListEquals(listBuild("a", "=", "b"), Tokenizer.tokenize("a=b"));
		assertListEquals(listBuild("a", "=", "b"), Tokenizer.tokenize(" a  =  b  "));
		assertListEquals(listBuild("a", "=", "abc"), Tokenizer.tokenize(" a  = \"abc\""));
		assertListEquals(listBuild("a", "=", "a b c"), Tokenizer.tokenize(" a  = \"a b c\""));
	}
	
	private List<String> listBuild(String... str) {
		List<String> l = new ArrayList<String>();
		for (int i=0; i<str.length; i++) {
			l.add(str[i]);
		}
		return l;
	}

	private void assertListEquals(List<String> foo, List<String> bar) {
		System.err.print("Expected: ");
		for (int i=0; i<foo.size(); i++) {
			if (i>0) {
				System.err.print(";");
			}
			System.err.print(foo.get(i));
		}
		System.err.println();
		System.err.print("Got     : ");
		for (int i=0; i<bar.size(); i++) {
			if (i>0) {
				System.err.print(";");
			}
			System.err.print(bar.get(i));
		}
		System.err.println();
				
		Assert.assertEquals(foo.size(),  bar.size());
		for (int i=0; i<foo.size(); i++) {
			Assert.assertEquals(foo.get(i),  bar.get(i));
		}
	}
} 
