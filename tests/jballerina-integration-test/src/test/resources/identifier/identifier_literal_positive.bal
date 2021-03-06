// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

const ASSERTION_ERR_REASON = "AssertionError";

//initialize final variables
final float 'const_IL_123 = 77.80;
final float '\ \/\:\@\[\`\{\~\u{2324}_IL = 88.90;
final float 'üňĩćőđę_ƈȏɳʂʈ_IL = 99.10;

//initialize global variables
string 'global_var_123 = "IL with global var";
string '\ \/\:\@\[\`\{\~\u{2324}_global_var = "IL with special characters in global var";
string 'üňĩćőđę_ĠĿŐΒȂɭ_var = "IL with unicode characters in global var";


function testFinalVariableIL() {
    assertEquality(77.80,'const_IL_123);
    assertEquality(88.90,'\ \/\:\@\[\`\{\~\u{2324}_IL);
    assertEquality(99.10,'üňĩćőđę_ƈȏɳʂʈ_IL);
}

function testGlobalVariableIL() {
    assertEquality("IL with global var",'global_var_123 );
    assertEquality("IL with special characters in global var",'\ \/\:\@\[\`\{\~\u{2324}_global_var);
    assertEquality("IL with unicode characters in global var",'üňĩćőđę_ĠĿŐΒȂɭ_var);
}

function testLocalVariableIL() {
    string 'local_var_123 = 'global_var_123;
    string '\ \/\:\@\[\`\{\~\u{2324}_local_var = '\ \/\:\@\[\`\{\~\u{2324}_global_var;
    string 'üňĩćőđę_ɬȭςαʆ_var = "IL with unicode characters in local var";
    assertEquality("IL with global var",'local_var_123 );
    assertEquality("IL with special characters in global var",'\ \/\:\@\[\`\{\~\u{2324}_local_var);
    assertEquality("IL with unicode characters in local var",'üňĩćőđę_ɬȭςαʆ_var);
}

type Person record {
    string '1st_name;
    string '\ \/\:\@\[\`\{\~\u{2324}_last_name;
    int 'Ȧɢέ;
};

function useILWithinStruct() {
    Person person = {'1st_name: "Tom", '\ \/\:\@\[\`\{\~\u{2324}_last_name:"Hank", 'Ȧɢέ: 50};
    assertEquality("Tom",person.'1st_name);
    assertEquality("Hank",person.'\ \/\:\@\[\`\{\~\u{2324}_last_name);
    assertEquality(50,person.'Ȧɢέ);
}

function useILInStructVar() {
    Person 'ƮέŞŢ_Person_\ \/\:\@\[\`\{\~\u{2324} =
    {'1st_name: "Harry", '\ \/\:\@\[\`\{\~\u{2324}_last_name:"Potter", 'Ȧɢέ: 22};

    assertEquality("Harry",'ƮέŞŢ_Person_\ \/\:\@\[\`\{\~\u{2324}.'1st_name);
    assertEquality("Potter",'ƮέŞŢ_Person_\ \/\:\@\[\`\{\~\u{2324}.'\ \/\:\@\[\`\{\~\u{2324}_last_name);
    assertEquality(22,'ƮέŞŢ_Person_\ \/\:\@\[\`\{\~\u{2324}.'Ȧɢέ);
}

type '\ \/\:\@\[\`\{\~\u{2324}_123_ƮέŞŢ_Person record {
    string name;
};

function useILInStructName() {
    '\ \/\:\@\[\`\{\~\u{2324}_123_ƮέŞŢ_Person person = {name: "Jack"};
    assertEquality("Jack",person.name);
}

function '\ \/\:\@\[\`\{\~\u{2324}_123_ƮέŞŢ_IL_function(string val) returns string {
    string s = " with IL function name";
    return val + s;
}

function testFunctionNameWithIL() {
     assertEquality("test with IL function name", '\ \/\:\@\[\`\{\~\u{2324}_123_ƮέŞŢ_IL_function("test"));
}

function passILValuesToFunction() {
    string '1st_Name = "Bill";
    string '\ \/\:\@\[\`\{\~\u{2324}_last_name = "Kary";
    int 'Ȧɢέ = 40;

    string expected = "first name :Bill, last name :Kary, age :40";
    assertEquality(expected,passILValuesAsParams('1st_Name, '\ \/\:\@\[\`\{\~\u{2324}_last_name, 'Ȧɢέ));
}

function passILValuesAsParams(string '1st_Arg, string '\ \/\:\@\[\`\{\~\u{2324}_arg2, int 'üňĩćőđę_arg3)
returns string {
    string result = "first name :" + '1st_Arg +
    ", last name :" + '\ \/\:\@\[\`\{\~\u{2324}_arg2 +
    ", age :" + 'üňĩćőđę_arg3.toString();
    return result;
}

public type '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Connector client object {
    boolean action2Invoked = false;

    public function init(string param1, string param2, int param3) {

    }

    public remote function action1() returns string {
        string 'sample_String_1 = "this ";
        string '\ \/\:\@\[\`\{\~\u{2324}_var = "is ";
        string 'üňĩćőđę_var = "action 1";
        return 'sample_String_1 + '\ \/\:\@\[\`\{\~\u{2324}_var + 'üňĩćőđę_var;
    }

    public remote function '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Action() returns string {
        string 'sample_String_2 = "this ";
        string '\ \/\:\@\[\`\{\~\u{2324}_var2 = "is ";
        string 'üňĩćőđę_var2 = "action 2";
        return 'sample_String_2 + '\ \/\:\@\[\`\{\~\u{2324}_var2 + 'üňĩćőđę_var2;
    }
};

function testConnectorNameWithIL() {
    '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Connector testConnector = new("MyParam1", "MyParam2", 5);
    string value = testConnector->action1();
    assertEquality("this is action 1", value);
}

function testConnectorActionWithIL() {
    '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Connector testConnector = new("MyParam1", "MyParam2", 5);
    string value = testConnector->'\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Action();
    assertEquality("this is action 2", value);
}

function useILAsArrayIndex() {
    float[] '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Array = [234.0, 8834.834, 88.0];
    int '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Array_index = 1;
    float value = '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Array ['\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Array_index];
    assertEquality(8834.834, value);
}

function useILAsWorkerName() {
     worker '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Worker returns string {
         string 'var1_\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ = "sample result";
         return "this is a " + 'var1_\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ;
     }
     string result  = wait '\ \/\:\@\[\`\{\~\u{2324}_ƮέŞŢ_Worker;
     assertEquality("this is a sample result", result);
}

function testWorker () returns int {
    worker '\u{2324}\ \/\:\@\[\`\{\~_ƮέŞŢ_Worker1 returns int {
        int result = 0;
        int i = 10;
        i -> '\u{2324}\ \/\:\@\[\`\{\~_ƮέŞŢ_Worker2;
        result = <- '\u{2324}\ \/\:\@\[\`\{\~_ƮέŞŢ_Worker2;
        return result;
    }

    worker '\u{2324}\ \/\:\@\[\`\{\~_ƮέŞŢ_Worker2 {
        int r = 120;
        int i = 0;
        i = <- '\u{2324}\ \/\:\@\[\`\{\~_ƮέŞŢ_Worker1;
        r = changeMessage(i);
        r -> '\u{2324}\ \/\:\@\[\`\{\~_ƮέŞŢ_Worker1;
    }
    return wait '\u{2324}\ \/\:\@\[\`\{\~_ƮέŞŢ_Worker1;
}

function changeMessage (int i) returns int {
    return i + 10;
}

function testWorkerInteractionWithIL() {
     int result  = testWorker();
     assertEquality(20, result);
}

public function main() {
    testFinalVariableIL();
    testGlobalVariableIL();
    testLocalVariableIL();
    useILWithinStruct();
    useILInStructVar();
    useILInStructName();
    testFunctionNameWithIL();
    passILValuesToFunction();
    testConnectorNameWithIL();
    testConnectorActionWithIL();
    useILAsArrayIndex();
    useILAsWorkerName();
    testWorkerInteractionWithIL();
}

function assertEquality(any|error expected, any|error actual) {
    if expected is anydata && actual is anydata && expected == actual {
        return;
    }
    if expected === actual {
        return;
    }
    panic error(ASSERTION_ERR_REASON,
                message = "expected '" + expected.toString() + "', found '" + actual.toString () + "'");
}
