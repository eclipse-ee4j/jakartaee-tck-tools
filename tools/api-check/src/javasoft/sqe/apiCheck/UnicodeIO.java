/*
 * Copyright (c) 2000, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */


package javasoft.sqe.apiCheck;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;



class UFileWriter extends FileWriter
{
    UFileWriter (String s) throws IOException
    {
        super(s);
    }


    public 
    void write(char cbuf[], int off, int len) throws IOException
    {
        int end = off + len;

        if (len == 0)
            return;

        if (off < 0 || len < 0 || end > cbuf.length)
            throw new IndexOutOfBoundsException();           

        int i = off,
            k = i;

        while (k < end)
        {
            if (cbuf[k] > 127)
            {
                super.write(cbuf, i, k-i);
                super.write(Esc(cbuf[k]));
                i = k + 1;
                k = i;
            }
            else
                k++;
        }

        super.write(cbuf, i, k-i);
    }


    static 
    String Esc (char c)
    {
        String s = Integer.toHexString(c);
        int n = s.length();

        if (n == 1)
            return "\\u000" + s;
        else if (n == 2)
            return "\\u00" + s;
        else if (n == 3)
            return "\\u0" + s;
        else
            return "\\u" + s;
    }
}


class UFileReader extends FileReader
{
    UFileReader (String s) throws IOException
    {
        super(s);
    }


    public 
    int read(char cbuf[], int off, int len) throws IOException
    {
        if (len == 0)
            return 0;

        if (off < 0 || len < 0 || off + len > cbuf.length)
            throw new IndexOutOfBoundsException();     
      
        int n = super.read(cbuf, off, len);
        if (n > 5)
        {
            int d1, d2, d3, d4,
                end = off + n - 5;

            for (int i = off; i < end; i++)
                if (cbuf[i] == '\\' 
                 && cbuf[i+1] == 'u' 
                 && (d1 = Character.digit(cbuf[i+2], 16)) != -1
                 && (d2 = Character.digit(cbuf[i+3], 16)) != -1
                 && (d3 = Character.digit(cbuf[i+4], 16)) != -1
                 && (d4 = Character.digit(cbuf[i+5], 16)) != -1)
                {
                    //System.err.println("esc: "+new String(cbuf, i, 6));
                    int d = (((((d1 << 4) + d2) << 4) + d3) << 4) + d4;
                    cbuf[i] = (char)d;

                    for (int k = i+1; k < end; k++)
                        cbuf[k] = cbuf[k+5];

                    n -= 5;
                    end -= 5;
                }
        }

        return n;
    }
}
