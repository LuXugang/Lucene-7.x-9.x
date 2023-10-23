(function () {
    if (window.location.protocol === 'file:' || $axure.player.isCloud) return;

    var inited = false;
    $axure.messageCenter.addMessageListener(function (message) {
        if (message == "finishInit") {
            setTimeout(() => {
                if (!inited) processFromQueue(0);
                inited = true;
            }, 1000); // give 1 sec to load all prototype data
        }
    });

    var queue = [processPagesHint, processNotesHint, processConsoleHint];

    function processFromQueue(index) {
        if (index < queue.length) queue[index](index);
    }

    function processPagesHint(currentIndex) {
        if ($axure.document.sitemap.rootNodes.length > 1 && !$axure.document.configuration.isAxshare) {
            $.get('shouldShowSitemapHint', function (shouldShow) {
                if (shouldShow) {
                    var pagesHint = $("<div class='pluginHint left'><span>Click to expand the sitemap and view your project pages.</span></div>");
                    pagesHint.click(e => e.stopPropagation());
                    var gotItBtn = $("<button class='ax-button'>Got It</button>");
                    pagesHint.append(gotItBtn);
                    $("#sitemapHostBtn").append(pagesHint);
                    var sitemapControlFrameContainer = $("#sitemapControlFrameContainer");
                    sitemapControlFrameContainer.click(gotIt);
                    gotItBtn.click(gotIt);

                    function gotIt(e) {
                        e.stopPropagation();
                        $.get('sitemapHintAccepted');
                        pagesHint.remove();
                        sitemapControlFrameContainer.off("click", gotIt);
                        processFromQueue(currentIndex + 1);
                    }
                }
            }).fail(function () {
                processFromQueue(currentIndex + 1);
            });
        } else {
            processFromQueue(currentIndex + 1);
        }
    }

    function processNotesHint(currentIndex) {
        var hasNotes = $axure.document.configuration.showPageNotes && (($axure.page.notes && !$.isEmptyObject($axure.page.notes)) || ($axure.page.masterNotes && $axure.page.masterNotes.length > 0) || ($axure.page.widgetNotes && $axure.page.widgetNotes.length > 0));
        if (hasNotes && !$axure.document.configuration.isAxshare) {
            $.get('shouldShowPageNoteHint', function (shouldShow) {
                if (shouldShow) {
                    var notesHint = $("<div class='pluginHint'><span>Click to view the Page and Widget Notes on this page.</span></div>");
                    notesHint.click(e => e.stopPropagation());
                    var gotItBtn = $("<button class='ax-button'>Got It</button>");
                    notesHint.append(gotItBtn);
                    var pluginBtn = $("#pageNotesHostBtn").append(notesHint);
                    pluginBtn.click(gotIt);
                    gotItBtn.click(gotIt);

                    function gotIt(e) {
                        e.stopPropagation();
                        $.get('pageNoteHintAccepted');
                        notesHint.remove();
                        pluginBtn.off("click", gotIt);
                        processFromQueue(currentIndex + 1);
                    }
                }
            }).fail(function () {
                processFromQueue(currentIndex + 1);
            });
        } else {
            processFromQueue(currentIndex + 1);
        }
    }

    function processConsoleHint() {
        if (!$axure.document.configuration.showConsole || $axure.document.configuration.isAxshare) return;

        $.get('shouldShowConsoleHint', function (shouldShow) {
            if (shouldShow) {
                var consoleHint = $("<div class='pluginHint right'><span><b>Reminder:</b> Use the console to see when events and actions are performed.</span></div>");
                var btn = $("#debugHostBtn").append(consoleHint);

                var delay = 4000;
                function runTimeout() {
                    return setTimeout(() => {
                        consoleHint.remove();
                    }, delay);
                }
                var timeoutId = runTimeout();
                btn.mouseenter(() => { clearTimeout(timeoutId); });
                btn.mouseleave(() => { timeoutId = runTimeout(); });
                btn.click(() => {
                    clearTimeout(timeoutId);
                    consoleHint.remove();
                });
            }
        });
    }
})();
