// use this to isolate the scope
(function () {

    if(!$axure.document.configuration.showConsole) { return; }

    $(document).ready(function () {
        var pluginInfo = {
            id: 'debugHost',
            context: 'inspect',
            title: 'Interaction Console',
            gid: 3
        };
        var pluginStarted = false;
        var showEmptyState = true;
        $axure.player.createPluginHost(pluginInfo);
        var prevElId = 'p';
        var currentElId = 'c';

        generateDebug();

        $('#variablesClearLink').click(clearvars_click);
        $('#traceClear').click(cleartrace_click);
        $('#traceToggle').click(stoptrace_click);

        $('#closeConsole').click(close);

        var currentStack= [];
        var finishedStack = [];

        $axure.messageCenter.addMessageListener(function (message, data) {
            if(message == 'axCompositeEventMessage') {
                for(var i = 0; i < data.length; i++) {
                    processMessages(data[i].message, data[i].data);
                }
            } else processMessages(message, data);
        });

        var processMessages = function (message, data) {
            if(message == 'openPlugin') {
                if(data == pluginInfo.id && !pluginStarted) {
                    starttrace();
                }
            } else if(message == 'globalVariableValues') {
                $('#variablesDiv').empty();
                for(var key in data) {
                    var value = data[key] == '' ? '(blank)' : data[key];
                    $('#variablesDiv').append('<div class="variableList"><div class="variableName">' + key + '</div><div class="variableValue">' + value + '</div></div>');
                }
            } else if(message == 'axEvent') {
                hideEmptyState();
                prevElId = currentElId;
                currentElId = data.elementId;

                var addToStack = "<div class='axEventBlock'>";
                addToStack += "<div class='axEventContainer'>";
                addToStack += "    <div class='axTime'>" + new Date().toLocaleTimeString() + "</div>";
                addToStack += "    <div class='axLabel'>" + data.label + " (" + data.type + ")</div>";
                addToStack += "    <div class='axEvent'>" + data.event.description + "</div>";
                addToStack += "</div></div>";

                currentStack.push($(addToStack));
            } else if (message == 'axEventComplete') {
                handleNoCondition()
                if (tryAddGroupCounter()) {
                    currentStack.pop();
                    return;
                }

                finishedStack.push(currentStack.pop());
                if(currentStack.length == 0) {
                    for(var i = finishedStack.length - 1; i >= 0; i--) {
                        if($('#traceDiv').children().length > 99) $('#traceDiv').children().last().remove();
                        $('#traceDiv').prepend(finishedStack[i]);
                    }
                    finishedStack = [];
                }
            } else if (message == 'axCase') {
                var addToStack = "<div class='axCaseContainer'>";
                addToStack += "    <div class='axCaseItem'>" + data.item + "</div>";
                if (data.description) { addToStack += "    <div class='axCaseDescription' title='" + data.description + "'>" + data.description + "</div>" };
                addToStack += "</div>";

                currentStack[currentStack.length - 1].append($(addToStack));
            } else if (message == 'axAction') {
                var addToStack = "<div class='axActionContainer'>";
                addToStack += "    <div class='axActionItem'>" + data.name + "</div>";
                addToStack += "</div>";

                currentStack[currentStack.length - 1].append($(addToStack));
            } else if (message == 'axInfo') {
                var addToStack = "<div class='axInfoContainer'>";
                addToStack += "    <div class='axInfoItem'>" + data.item + "</div>";
                if (data.description) { addToStack += "    <div class='axInfoDescription' title='" + data.longDescription + "'>" + data.description + "</div>" };
                addToStack += "</div>";

                currentStack[currentStack.length - 1].append($(addToStack));
            }
        }

        // bind to the page load
        $axure.page.bind('load.debug', function () {
            var traceStr = $axure.player.getHashStringVar(TRACE_VAR_NAME);
            if (!traceStr) $axure.messageCenter.setState("isTracing", false);
            else if (traceStr == 1) starttrace();
            else if (traceStr == 0) stoptrace_click();
            $axure.messageCenter.postMessage('getGlobalVariables', '');
            return false;
        });

        function handleNoCondition() {
            var event = currentStack[currentStack.length - 1];
            var action = event.find('.axActionContainer');
            if (action.length == 0) {
                event.append($("<div class='axActionContainer'><span>No condition met</span></div></div>"));
            }
        }

        function compareEventBlocks(first, second) {
            if(currentElId !== prevElId) return false;
            var firstClone = first.clone();
            var secondClone = second.clone();
            firstClone.find('.axTime').remove();
            secondClone.find('.axTime').remove();
            firstClone.find('.axEventCounter').remove();
            secondClone.find('.axEventCounter').remove();
            return firstClone.html() === secondClone.html();
        }

        function tryAddGroupCounter() {
            var prevEvent;
            if(finishedStack.length == 0 && currentStack.length == 1) {
                prevEvent = $('#traceDiv').find('.axEventBlock').first();
                if(prevEvent.length == 0) return false;
            } else if(finishedStack.length > 0) {
                prevEvent = finishedStack[finishedStack.length - 1];
            } else {
                return false;
            }

            var currentEvent = currentStack[currentStack.length - 1];

            if(compareEventBlocks(prevEvent, currentEvent)) {
                var prevLabel = prevEvent.find('.axLabel');
                var counterBlock = prevLabel.find('.axEventCounter');
                prevEvent.find('.axTime').text(currentEvent.find('.axTime').text());
                if(counterBlock.length == 0) {
                    var eventCounter = "<span class='axEventCounter'>2</span>";
                    prevLabel.append($(eventCounter));
                    return true;
                }
                var count = counterBlock.text();
                if(isNaN(count)) return true;
                if(count > 8) counterBlock.text('9+');
                else counterBlock.text(+count + 1);
                return true;
            }
            return false;
        }

        function clearvars_click(event) {
            $axure.messageCenter.postMessage('resetGlobalVariables', '');
        }

        function close() {
            $axure.player.pluginClose("debugHost");
        }

        function cleartrace_click(event) {
            $('#traceDiv').html('');
            clearLastEventState();
        }

        function clearLastEventState() {
            lastEventId = '';
            sameLastEvent = false;
            lastCaseName = '';
        }

        function starttrace() {
            $axure.messageCenter.setState("isTracing", true);
            console.log("starting trace");
            $axure.player.setVarInCurrentUrlHash(TRACE_VAR_NAME, 1);
            pluginStarted = true;

            if (!$axure.document.configuration.isAxshare) {
                $.get("consoleShown");
            }
        }

        function hideEmptyState() {
            if(showEmptyState) {
                $('#traceEmptyState').hide();
                showEmptyState = false;
            }
        }

        function restarttrace_click(event) {
            $('#traceToggle').text('Stop Trace');
            $('#traceToggle').off("click");
            $('#traceToggle').click(stoptrace_click);
            starttrace();
            clearLastEventState();
        }

        function stoptrace_click(event) {
            $axure.messageCenter.setState("isTracing", false);
            $('#traceDiv').prepend('<div class="tracePausedNotification">Trace Paused<div>');
            $('#traceToggle').text('Restart Trace');
            $('#traceToggle').off("click");
            $('#traceToggle').click(restarttrace_click);
            console.log("stopping trace");
            $axure.player.setVarInCurrentUrlHash(TRACE_VAR_NAME, 0);
            pluginStarted = true;
        }
    });

    function generateDebug() {
        var pageNotesUi = "<div id='debugHeader'>";
        pageNotesUi += "<div id='debugToolbar'>";
        pageNotesUi += "<div id='consoleTitle' class='pluginNameHeader'>Console</div>";

        pageNotesUi += "</div>";
        pageNotesUi += "</div>";

        pageNotesUi += "<div id='variablesContainer' style='max-height:300px; overflow-y:auto'>";        
        pageNotesUi += "<div id='variablesTitle' class='sectionTitle'>Variables</div>";
        pageNotesUi += "<a id='variablesClearLink' class='traceOption'>Reset Variables</a>";
        pageNotesUi += "<div id='variablesDiv'></div></div>";
        pageNotesUi += "<div class='lineDivider'></div>";
        pageNotesUi += "<div id='traceContainer'>";

        pageNotesUi += "<div id='traceHeader'>";
        pageNotesUi += "<span class='sectionTitle'>Trace</span><a id='traceClear' class='traceOption'>Clear Trace</a><a id='traceToggle' class='traceOption'>Stop Trace</a>";
        pageNotesUi += "</div>";
        pageNotesUi += "</div>";
        pageNotesUi += "<div id='debugScrollContainer'>";
        pageNotesUi += "<div id='debugContainer'>";

        pageNotesUi += "<div id='traceEmptyState'>Interactions will be recorded here as you click through the prototype.</div>";
        pageNotesUi += "<div id='traceDiv'></div></div>";
        pageNotesUi += "</div></div>";

        $('#debugHost').append(pageNotesUi);
    }
})();
