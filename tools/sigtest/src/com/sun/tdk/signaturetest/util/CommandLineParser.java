/*
 * $Id: CommandLineParser.java 4504 2008-03-13 16:12:22Z sg215604 $
 *
 * Copyright 1996-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tdk.signaturetest.util;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Roman Makarchuk
 * @author Yuri Danilevich  
 */
public class CommandLineParser {

    private Object servicedObject;
    private KnownOptions knownOptions;
    private Map decoders = new HashMap();

    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(CommandLineParser.class);
    
    private Map foundOptions = new HashMap();

    public CommandLineParser(Object servicedObject, String optionPrefix) {
        this.servicedObject = servicedObject;
        knownOptions = new KnownOptions(optionPrefix);
    }

    public final void addOption(String option, OptionInfo info) {

        String temp = option;
        if (!info.isCaseSentitive())
            temp = option.toLowerCase();

        knownOptions.add(temp, info);
    }

    public final void addOption(String option, OptionInfo info, String decoder) {

        String temp = option;
        if (!info.isCaseSentitive())
            temp = option.toLowerCase();

        knownOptions.add(temp, info);
        decoders.put(temp, decoder);
    }

    public final void removeKnownOption(String option) {
        knownOptions.remove(option);
    }

    public void processArgs(String args[]) throws CommandLineParserException {

        foundOptions.clear();

        String optionStr = null;

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (knownOptions.isKnownOption(arg)) {
                OptionInfo ki = knownOptions.get(arg);
                optionStr = ki.toKey(arg);

                ArrayList params = (ArrayList) foundOptions.get(optionStr);

                if (params == null) {
                    foundOptions.put(optionStr, new ArrayList());
                } else if (!ki.isMultiple()) {
                    throw new CommandLineParserException(i18n.getString("CommandLineParser.error.option.duplicate", optionStr));
                }

            } else if (!knownOptions.isOption(arg)) {
                if (optionStr != null)
                    ((List) foundOptions.get(optionStr)).add(arg);
            } else {
//                optionStr = null;
                throw new CommandLineParserException(i18n.getString("CommandLineParser.error.option.unknown", arg));
            }
        }

        knownOptions.validate(foundOptions);

        Iterator it = foundOptions.keySet().iterator();
        while (it.hasNext()) {
            String foundOption = (String) it.next();
            invokeDecoder(foundOption, (ArrayList) foundOptions.get(foundOption));
        }
    }

    public boolean isOptionSpecified(String arg, String checkedOption) {
        return knownOptions.isOption(arg) && checkedOption.equalsIgnoreCase(arg);
    }

    // Note: USE ONLY KNOWN OPTIONS as ARGUMENT OF THIS METHOD!
    // Option should not has the prefix
    public boolean isOptionSpecified(String option) {

        if (!knownOptions.isKnownOption(option))
            throw new IllegalArgumentException(i18n.getString("CommandLineParser.error.option.unknown", option));

        OptionInfo ki = knownOptions.get(option);
        String temp = ki.toKey(option);
        return foundOptions.get(temp) != null;
    }

    private void invokeDecoder(String option, ArrayList params) throws CommandLineParserException {

        String decoder = (String) decoders.get(option);
        if (decoder != null)
            invokeExplicitDecoder(decoder, option, params);
        else
            invokeDefaultDecoder(option, params);
    }

    private void invokeExplicitDecoder(String decoder, String option, ArrayList params) throws CommandLineParserException {

        Class cl = servicedObject.getClass();

        String[] stemp = new String[params.size()];
        for (int i = 0; i < params.size(); ++i)
            stemp[i] = (String) params.get(i);

        try {
            Method method = cl.getMethod(decoder, new Class[]{String.class, String[].class});
            method.invoke(servicedObject, new Object[]{option, stemp});

        }
        catch (NoSuchMethodException nsme) {
            throw new CommandLineParserException(i18n.getString("CommandLineParser.error.decoder.explicit.notfound", new Object[] {decoder, option, cl.getName()}), nsme);
        }
        catch(InvocationTargetException e) {
            Throwable th = e.getTargetException();
            String message;
            if (th instanceof CommandLineParserException)
                message = th.getMessage();
            else
                message = i18n.getString("CommandLineParser.error.decoder.failed", new Object[] {option, th});           
            throw new CommandLineParserException(message);
        }
        catch (Exception e) {
            throw new CommandLineParserException(i18n.getString("CommandLineParser.error.decoder.failed", new Object[] {option, e}));
        }
    }

    private void invokeDefaultDecoder(String option, ArrayList params) throws CommandLineParserException {
        try {
            String[] stemp = new String[params.size()];
            for (int i = 0; i < params.size(); ++i)
                stemp[i] = (String) params.get(i);

            getDefaultDecoderMethod(option).invoke(servicedObject, new Object[]{stemp});
        } catch (Exception e) {
            throw new CommandLineParserException(i18n.getString("CommandLineParser.error.decoder.failed", new Object[] {option, e}));
        }
    }

    private boolean isDecoder(Method method, String option) {
        String methodName = "decode" + option;
        return method.getName().equalsIgnoreCase(methodName) &&
                method.getParameterTypes().length == 1 &&
                method.getParameterTypes()[0].isAssignableFrom(String[].class);
    }

    private Method getDefaultDecoderMethod(String option) throws CommandLineParserException {

        Method m = getDefaultDecoderMethod(servicedObject.getClass().getMethods(), option);

        if (m == null)
            throw new CommandLineParserException(i18n.getString("CommandLineParser.error.decoder.default.notfound", new Object[] {option, servicedObject.getClass().getName()}));

        return m;
    }

    private Method getDefaultDecoderMethod(Method[] methods, String option) {

        for (int i = 0; i < methods.length; ++i) {
            if (isDecoder(methods[i], option))
                return methods[i];
        }

        return null;
    }

    private static class KnownOptions {
        private Map data = new HashMap();

        private final String optionPrefix;

        public KnownOptions(String optionPrefix) {
            this.optionPrefix = optionPrefix;
        }

        private boolean isKnownOption(String arg) {

            if (isOption(arg)) {
                String temp = arg;

                if (!data.containsKey(temp)) {
                    temp = temp.toLowerCase();
                    OptionInfo ki = (OptionInfo) data.get(temp);
                    if (ki != null)
                        return !ki.isCaseSentitive();
                } else return true;
            }
            return false;
        }

        private void add(String option, OptionInfo info) {
            if (!option.startsWith(optionPrefix))
                throw new IllegalArgumentException(i18n.getString("CommandLineParser.error.option.noprefix", optionPrefix));
            data.put(option, info);
        }

        private void remove(String option) {
            data.remove(option);
        }

        private OptionInfo get(String option) {

            String temp = option;
            OptionInfo ki = (OptionInfo) data.get(temp);

            if (ki == null) {
                temp = temp.toLowerCase();
                ki = (OptionInfo) data.get(temp);

                if (ki == null || ki.isCaseSentitive())
                    return null;
            }

            return ki;
        }

        private void validateRequiredOptions(Set foundKeys) throws CommandLineParserException {
            Set keySet = data.keySet();

            Iterator it = keySet.iterator();
            while (it.hasNext()) {
                String option = (String) it.next();
                OptionInfo ki = (OptionInfo) data.get(option);
                if (ki.isRequired() && !foundKeys.contains(option))
                    throw new CommandLineParserException(i18n.getString("CommandLineParser.error.option.required", option));
            }
        }

        private void validateCount(String option, int paramCount) throws CommandLineParserException {
            OptionInfo info = (OptionInfo) data.get(option);
            int minCount = info.getMinCount();
            int maxCount = info.getMaxCount();

            if (paramCount < minCount)
                throw new CommandLineParserException(i18n.getString("CommandLineParser.error.option.require_more_parameters", new Object[] {option, new Integer(minCount)}));

            if (paramCount > maxCount) {

                String msg = i18n.getString("CommandLineParser.error.option.require_less_parameters", new Object[] {option, new Integer(maxCount)});

                if (maxCount == 0)
                    msg = i18n.getString("CommandLineParser.error.option.require_no_parameters", option);

                throw new CommandLineParserException(msg);
            }
        }

        private void validate(Map params) throws CommandLineParserException {
            validateRequiredOptions(params.keySet());

            Iterator it = params.keySet().iterator();
            while (it.hasNext()) {
                String option = (String) it.next();
                validateCount(option, ((List) params.get(option)).size());
            }
        }

        public boolean isOption(String arg) {
            return arg.startsWith(optionPrefix);
        }
    }
}
