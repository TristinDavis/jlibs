/**
 * Copyright 2015 Santhosh Kumar Tekuri
 *
 * The JLibs authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package jlibs.nblr.codegen;

import jlibs.core.annotation.processing.Printer;
import jlibs.nblr.codegen.java.SyntaxClass;
import jlibs.nblr.matchers.Any;
import jlibs.nblr.matchers.Matcher;
import jlibs.nblr.matchers.Not;
import jlibs.nblr.matchers.Range;
import jlibs.nblr.rules.Answer;
import jlibs.nbp.NBParser;

import static jlibs.core.annotation.processing.Printer.MINUS;
import static jlibs.core.annotation.processing.Printer.PLUS;

/**
 * @author Santhosh Kumar T
 */
public class FinishAllMethod{
    Matcher matcher;
    String methodName;
    public boolean returnValueRequired = false;
    public Answer buffering;

    public FinishAllMethod(Matcher matcher, String methodName){
        this.matcher = matcher;
        this.methodName = methodName;
        if(SyntaxClass.FINISH_ALL.equals(methodName) || SyntaxClass.FINISH_ALL_OTHER_THAN.equals(methodName))
            returnValueRequired = true;
        else
            returnValueRequired = matcher.clashesWith(Range.SUPPLIMENTAL) || matcher.clashesWith(Any.NEW_LINE);
    }

    public void use(Printer printer, State state, boolean returnValueRequired){
        String ch = state.readMethod();
        String methodCall;
        if(methodName.equals(SyntaxClass.FINISH_ALL) || methodName.equals(SyntaxClass.FINISH_ALL_OTHER_THAN)){
            Any any = (Any)(methodName.equals(SyntaxClass.FINISH_ALL_OTHER_THAN) ? ((Not)matcher).delegate : matcher);
            methodCall = methodName+"("+ch+", "+Matcher.toJava(any.chars[0])+')';
        }else{
            if(!matcher.clashesWith(Range.SUPPLIMENTAL) && !matcher.clashesWith(Any.NEW_LINE))
                ch = "";
            else if(matcher.clashesWith(Range.NON_SUPPLIMENTAL))
                ch = "";
            methodCall = "finishAll_"+methodName+"("+ch+")";
        }

        if(returnValueRequired)
            methodCall = "(ch="+methodCall+")";

        String condition = methodCall+"==EOC";
        if(!this.returnValueRequired)
            condition = methodCall;

        printer.printlns(
            "if("+condition+")",
                PLUS,
                state.breakStatement(),
                MINUS
        );
    }

    public void generate(Printer printer){
        String condition = matcher._javaCode("ch");
        String _condition = condition;
        if(matcher.checkFor(NBParser.EOF) || matcher.checkFor(NBParser.EOC))
            condition = "ch>=0 && "+condition;

        if(!matcher.clashesWith(Range.SUPPLIMENTAL) && !matcher.clashesWith(Any.NEW_LINE)){
            condition = matcher._javaCode("ch");
            String returnType = returnValueRequired ? "int" : "boolean";
            String returnValue = returnValueRequired ? "codePoint()" : "position==limit && marker==EOC";
            printer.printlns(
                "private "+returnType+" finishAll_"+methodName+"() throws IOException{",
                    PLUS,
                    "int _position = position;",
                    "while(position<limit){",
                        PLUS,
                        "char ch = input[position];",
                        "if("+condition+")",
                            PLUS,
                            "++position;",
                            MINUS,
                        "else",
                            PLUS,
                            "break;",
                            MINUS,
                        MINUS,
                    "}"
                );
                if(buffering!=Answer.NO){
                    String isBuffering = buffering==Answer.MAY_BE ? " && buffer.isBuffering()" : "";
                    printer.printlns(
                        "int len = position-_position;",
                        "if(len>0"+isBuffering+")",
                            PLUS,
                            "buffer.append(input, _position, len);",
                            MINUS
                    );
                }
                printer.printlns(
                    "return "+returnValue+";",
                    MINUS,
                "}"
            );
        }else if(!matcher.clashesWith(Range.NON_SUPPLIMENTAL)){
            printer.printlns(
                "private int finishAll_"+methodName+"(int ch) throws IOException{",
                    PLUS,
                    "while("+condition+"){",
                        PLUS,
                        "consume(ch);",
                        "ch = codePoint();",
                        MINUS,
                    "}",
                    "return ch;",
                    MINUS,
                 "}"
            );
        }else{
            printer.printlns(
                "private int finishAll_"+methodName+"() throws IOException{",
                    PLUS,
                    "int ch;"
            );

            if(matcher.clashesWith(Range.SUPPLIMENTAL)){
                printer.printlns(
                    "while(true){",
                        PLUS
                );
            }
            printer.printlns(
                "asciiLoop: while(true){",
                    PLUS
            );

            String max = "limit";
            if(buffering!=Answer.NO){
                max = "max";
                printer.printlns(
                    "char chars[] = buffer.array();",
                    "int max = position + chars.length-buffer.count;",
                    "if(limit<max)",
                        PLUS,
                        "max = limit;",
                        MINUS
                );
            }
            printer.printlns(
                "while(position<"+max+"){",
                    PLUS,
                    "ch = input[position];"
            );

            boolean addElse = false;
            if(matcher.clashesWith(new Any('\r'))){
                printer.printlns(
                    "if(ch=='\\r'){",
                        PLUS,
                        "line++;",
                        "linePosition = ++position;"
                );

                if(buffering!=Answer.NO){
                    if(buffering==Answer.MAY_BE){
                        printer.printlns(
                            "if(buffer.isBuffering())",
                                PLUS
                        );
                    }
                    printer.printlns("chars[buffer.count++] = coalesceNewLines ? '\\n' : '\\r';");
                    if(buffering==Answer.MAY_BE){
                        printer.printlns(
                                MINUS
                        );
                    }
                }

                printer.printlns(
                        MINUS,
                    "}"
                );
                addElse = true;
            }
            if(matcher.clashesWith(new Any('\n'))){
                if(addElse)
                    printer.print("else ");
                printer.printlns(
                    "if(ch=='\\n'){",
                        PLUS,
                        "linePosition = ++position;",
                        "char lastChar = position==start+1 ? this.lastChar : input[position-2];",
                        "if(lastChar!='\\r')",
                            PLUS,
                            "line++;",
                            MINUS
                );

                if(buffering!=Answer.NO){
                    printer.printlns(
                        "else if(coalesceNewLines)",
                            PLUS,
                            "continue;",
                            MINUS
                    );

                    if(buffering==Answer.MAY_BE){
                        printer.printlns(
                            "if(buffer.isBuffering())",
                                PLUS
                        );
                    }
                    printer.printlns("chars[buffer.count++] = '\\n';");
                    if(buffering==Answer.MAY_BE){
                        printer.printlns(
                                MINUS
                        );
                    }
                }

                printer.printlns(
                        MINUS,
                    "}"
                );
                addElse = true;
            }

            if(addElse)
                printer.print("else ");
            printer.printlns(
                "if("+_condition+"){",
                    PLUS
            );
            if(buffering!=Answer.NO){
                if(buffering==Answer.MAY_BE){
                    printer.printlns(
                        "if(buffer.isBuffering())",
                            PLUS
                    );
                }
                printer.printlns("chars[buffer.count++] = (char)ch;");
                if(buffering==Answer.MAY_BE){
                    printer.printlns(
                            MINUS
                    );
                }
            }
            printer.printlns(
                    "position++;",
                    MINUS,
                "}else if(ch>=MIN_HIGH_SURROGATE && ch<=MAX_HIGH_SURROGATE)",
                    PLUS,
                    "break asciiLoop;",
                    MINUS,
                "else{",
                    PLUS,
                    "increment = 1;",
                    "return ch;",
                    MINUS,
                "}"
            );

            // end inner while
            printer.printlns(
                        MINUS,
                    "}",
                    "if(position==limit)",
                        PLUS,
                        "return marker;",
                        MINUS,
                    "buffer.expandCapacity(1);",
                    MINUS,
                "}"
            );

            if(matcher.clashesWith(Range.SUPPLIMENTAL)){
                printer.printlns(
                    "ch = codePoint();",
                    "if("+condition+")",
                        PLUS,
                        "consume(ch);",
                        MINUS,
                    "else",
                        PLUS,
                        "return ch;",
                        MINUS,
                        MINUS,
                    "}"
                );
            }else
                printer.println("return codePoint();");

            // end method
            printer.printlns(
                    MINUS,
                 "}"
            );
        }
    }
}