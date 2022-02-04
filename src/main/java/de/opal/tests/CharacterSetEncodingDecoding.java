package de.opal.tests;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

public class CharacterSetEncodingDecoding {

	public CharacterSetEncodingDecoding() {
		// TODO Auto-generated constructor stub
	}

	private static void test1() throws CharacterCodingException
	{
		// CharBuffer chars = CharBuffer.wrap(new char[] {'š', 'đ', 'č', 'Đ', '&',
		// 'ä'});
		// CharBuffer chars = CharBuffer.wrap(new char[] {'Ü', 'Ɖ', 'Đ', '&', 'ä', 'ö',
		// 'ü'});
		CharBuffer chars = CharBuffer.wrap(new char[] { 'Ā', 0x180 });
		// CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder();
		CharsetEncoder encoder = Charset.forName("Cp1250").newEncoder();
		// encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		ByteBuffer bytes = encoder.encode(chars);

		bytes.position(0);
		while (bytes.hasRemaining()) {
			System.out.print(bytes.get() + " ");
		}

	}

	private static void test2() {
		CharBuffer chars = CharBuffer.wrap(new char[] { 'Ā', 0x189 });
		System.out.println(chars);
	}
	
	private static void test3() throws CharacterCodingException {
		CharBuffer chars = CharBuffer.wrap(new char[] { 0x189 });
		System.out.println(chars);
		
		CharsetEncoder encoder = Charset.forName("Cp1250").newEncoder();
		// encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		ByteBuffer bytes = encoder.encode(chars);

		bytes.position(0);
		while (bytes.hasRemaining()) {
			System.out.print(bytes.get() + " ");
		}	
	}
	
	public static void main(String[] argv) throws Exception {
		//test1();
		//test2();
		test3();
	}
}
