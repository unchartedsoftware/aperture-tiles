define(['class'], function (Class) {
    "use strict";
    var TestObj, TestObjLog;

    TestObj = Class.extend({
        init: function () {
            this.v1 = 2;
            this.v2 = 3;
        },
        add: function () {
            return this.v1 + this.v2;
        }
    });
    TestObjLog = TestObj.extend({
        add: function () {
            return this.v1 * this.v2;
        }
    });

    return {TestObj: TestObj,
            TestObjLog: TestObjLog};
});
