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

package jlibs.core.net;

/**
 * @author Santhosh Kumar T
 */
public enum Protocol{
    TCP(-1, false, "Transmission Control Protocol"),
    SSL(-1, true, "Secure Socket Layer"),
    FTP(21, false, "File Transfer Protocol"),
    SSH(22, true, "Secure Shell"),
    TELNET(23, false, "Telnet protocol"),
    SMTP(25, false, "Simple Mail Transfer Protocol"),
    HTTP(80, false, "Hypertext Transfer Protocol"),
    HTTPS(443, true, "Hypertext Transfer Protocol Secure"),
    POP3(110, false, "Post Office Protocol v3"),
    IMAP(143, false, "Internet Message Access Protocol"),
    RMI(1099, false, "Remote Method Invocation"),
    CVS(2401, false, "Concurrent Versions System"),
    SVN(3690, false, "Subversion"),
    ;

    private int port;
    private String displayName;
    private boolean secured;
    Protocol(int port, boolean secured, String displayName){
        this.port = port;
        this.secured = secured;
        this.displayName = displayName;
    }

    public boolean secured(){
        return secured;
    }
    public int port(){
        return port;
    }
    public String displayName(){
        return displayName;
    }
}
