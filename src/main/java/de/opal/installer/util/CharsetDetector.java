package de.opal.installer.util;

/* http://www.java2s.com/Code/Java/I18N/Howtoautodetectafilesencoding.htm
 */

/*
 *  Copyright 2010 Georgios Migdos <cyberpython@gmail.com>.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
*
* @author Georgios Migdos <cyberpython@gmail.com>
*/
public class CharsetDetector {

   public Charset detectCharset(File f, String[] charsets) {

       Charset charset = null;

       for (String charsetName : charsets) {
           charset = detectCharset(f, Charset.forName(charsetName));
           if (charset != null) {
               break;
           }
       }

       return charset;
   }

   private Charset detectCharset(File f, Charset charset) {
       try {
           BufferedInputStream input = new BufferedInputStream(new FileInputStream(f));

           CharsetDecoder decoder = charset.newDecoder();
           decoder.reset();

           byte[] buffer = new byte[512];
           boolean identified = false;
           while ((input.read(buffer) != -1) && (!identified)) {
               identified = identify(buffer, decoder);
           }

           input.close();

           if (identified) {
               return charset;
           } else {
               return null;
           }

       } catch (Exception e) {
           return null;
       }
   }

   private boolean identify(byte[] bytes, CharsetDecoder decoder) {
       try {
           decoder.decode(ByteBuffer.wrap(bytes));
       } catch (CharacterCodingException e) {
           return false;
       }
       return true;
   }

   public static void main(String[] args) {
       File f = new File("example.txt");

       String[] charsetsToBeTested = {"UTF-8", "windows-1253", "ISO-8859-7"};

       CharsetDetector cd = new CharsetDetector();
       Charset charset = cd.detectCharset(f, charsetsToBeTested);

       if (charset != null) {
           try {
               InputStreamReader reader = new InputStreamReader(new FileInputStream(f), charset);
               int c = 0;
               while ((c = reader.read()) != -1) {
                   System.out.print((char)c);
               }
               reader.close();
           } catch (FileNotFoundException fnfe) {
               fnfe.printStackTrace();
           }catch(IOException ioe){
               ioe.printStackTrace();
           }

       }else{
           System.out.println("Unrecognized charset.");
       }
   }
}
   
