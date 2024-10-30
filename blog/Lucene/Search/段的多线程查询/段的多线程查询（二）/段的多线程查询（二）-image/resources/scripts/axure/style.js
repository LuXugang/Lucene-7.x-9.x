$axure.internal(function($ax) {
    var _style = {};
    $ax.style = _style;

    var _disabledWidgets = {};
    var _selectedWidgets = {};
    var _errorWidgets = {};
    var _hintWidgets = {};

    // A table to cache the outerHTML of the _rtf elements before the rollover state is applied.
    var _originalTextCache = {};
    // A table to exclude the normal style from adaptive overrides
    var _shapesWithSetRichText = {};

    // just a listing of shape ids
    var _adaptiveStyledWidgets = {};

    var _setLinkStyle = function(id, styleName) {
        var parentId = $ax.GetParentIdFromLink(id);
        var style = _computeAllOverrides(id, parentId, styleName, $ax.adaptive.currentViewId);

        var textId = $ax.GetTextPanelId(parentId);
        if(!_originalTextCache[textId]) {
            $ax.style.CacheOriginalText(textId);
        }
        if($.isEmptyObject(style)) return;

        var textCache = _originalTextCache[textId].styleCache;

        _transformTextWithVerticalAlignment(textId, function() {
            var cssProps = _getCssStyleProperties(style);
            $('#' + id).find('*').addBack().each(function(index, element) {
                element.setAttribute('style', textCache[element.id]);
                _applyCssProps(element, cssProps);
            });
        });
    };

    var _resetLinkStyle = function(id) {
        var textId = $ax.GetTextPanelId($ax.GetParentIdFromLink(id));
        var textCache = _originalTextCache[textId].styleCache;

        _transformTextWithVerticalAlignment(textId, function() {
            $('#' + id).find('*').addBack().each(function(index, element) {
                element.style.cssText = textCache[element.id];
            });
        });
        if($ax.event.mouseDownObjectId) {
            $ax.style.SetWidgetMouseDown($ax.event.mouseDownObjectId, true);
        } else if($ax.event.mouseOverObjectId) {
            $ax.style.SetWidgetHover($ax.event.mouseOverObjectId, true);
        }
    };

    $ax.style.SetLinkHover = function(id) {
        _setLinkStyle(id, MOUSE_OVER);
    };

    $ax.style.SetLinkNotHover = function(id) {
        _resetLinkStyle(id);
    };

    $ax.style.SetLinkMouseDown = function(id) {
        _setLinkStyle(id, MOUSE_DOWN);
    };

    $ax.style.SetLinkNotMouseDown = function(id) {
        _resetLinkStyle(id);
        var style = _computeAllOverrides(id, $ax.event.mouseOverObjectId, MOUSE_OVER, $ax.adaptive.currentViewId);

        if(!$.isEmptyObject(style)) $ax.style.SetLinkHover(id);
        //we dont do anything here because the widget not mouse down has taken over here
    };

    var _widgetHasState = function(id, state) {
        if($ax.style.getElementImageOverride(id, state)) return true;
        var diagramObject = $ax.getObjectFromElementId(id);

        //var adaptiveIdChain = $ax.adaptive.getAdaptiveIdChain($ax.adaptive.currentViewId);
        var adaptiveIdChain = $ax.style.getViewIdChain($ax.adaptive.currentViewId, id, diagramObject);

        for(var i = 0; i < adaptiveIdChain.length; i++) {
            var viewId = adaptiveIdChain[i];
            var adaptiveStyle = diagramObject.adaptiveStyles[viewId];
            if(adaptiveStyle && adaptiveStyle.stateStyles && adaptiveStyle.stateStyles[state]) return true;
        }

        if(diagramObject.style.stateStyles) {
            var stateStyle = diagramObject.style.stateStyles[state];
            if(!stateStyle) return false;
            return !$.isEmptyObject(stateStyle);
        }

        return false;
    };

    $ax.style.SetWidgetHover = function(id, value) {
        if(!_widgetHasState(id, MOUSE_OVER)) return;

        var valToSet = value || _isRolloverOverride(id);
        const state = _generateFullState(id, undefined, undefined, undefined, undefined, valToSet);
        _applyImageAndTextJson(id, state);
        _updateElementIdImageStyle(id, state);
    };

    var _rolloverOverrides = [];
    var _isRolloverOverride = function(id) {
        return _rolloverOverrides.indexOf(id) != -1;
    };

    $ax.style.AddRolloverOverride = function(id) {
        if(_isRolloverOverride(id)) return;
        _rolloverOverrides[_rolloverOverrides.length] = id;
        if($ax.event.mouseOverIds.indexOf(id) == -1) $ax.style.SetWidgetHover(id, true);
    };

    $ax.style.RemoveRolloverOverride = function(id) {
        var index = _rolloverOverrides.indexOf(id);
        if(index == -1) return;
        $ax.splice(_rolloverOverrides, index, 1);
        if($ax.event.mouseOverIds.indexOf(id) == -1) $ax.style.SetWidgetHover(id, false);
    };

    $ax.style.ObjHasMouseDown = function (id) {
        return _widgetHasState(id, MOUSE_DOWN);
    };

    // checkMouseOver is used for applying style effects to everything in a group
    $ax.style.SetWidgetMouseDown = function(id, value, checkMouseOver) {
        if($ax.style.IsWidgetDisabled(id)) return;
        if(!_widgetHasState(id, MOUSE_DOWN)) return;
        const state = _generateFullState(id, undefined, undefined, undefined, undefined, !checkMouseOver ? true : undefined, value);
        _applyImageAndTextJson(id, state);
        _updateElementIdImageStyle(id, state);
    };

    var _generateFullState = function (id, forceFocused, forceSelected, forceDisabled, forceError, forceMouseOver, forceMouseDown) {
        let state = $ax.style.ShowingHint(id) ? HINT : '';
        if (forceError === true || forceError !== false && _style.IsWidgetError(id)) {
            state = _compoundStates(state, ERROR);
        }
        if (forceSelected === true || forceSelected !== false && _style.IsWidgetSelected(id)) {
            state = _compoundStates(state, SELECTED);
        }
        if (forceDisabled === true || forceDisabled !== false && _isWidgetDisabled(id)) {
            return _compoundStates(DISABLED, state);
        }
        if (forceFocused === true || forceFocused !== false && _hasAnyFocusedClass(id)) {
            return _compoundStates(state, FOCUSED);
        }
        if (forceMouseDown === true || forceMouseDown !== false && $ax.event.mouseDownObjectId == id) {
            state = _compoundStates(state, MOUSE_DOWN);
        }
        if (forceMouseOver === true || forceMouseOver !== false &&
            ($ax.event.mouseOverIds.indexOf(id) !== -1 && _widgetHasState(id, MOUSE_OVER) || _isRolloverOverride(id))) {
            return _compoundStates(state, MOUSE_OVER);
        }
        return state.length > 0 ? state : NORMAL;
    };

    var _compoundStates = function (current, baseState) {
        if (current.length < 1) return baseState;
        if (baseState.length < 1) return current;
        const capital = current.charAt(0).toUpperCase() + current.slice(1);
        return baseState + capital;
    };

    $ax.style.updateImage = function (id) {

        const state = $ax.style.generateState(id);
        const imageUrl = $ax.adaptive.getImageForStateAndView(id, state);
        if (imageUrl) _applyImage(id, imageUrl, state);
        $ax.style.updateElementIdImageStyle(id, state);
    }

    $ax.style.SetWidgetFocused = function (id, value) {
        if (_isWidgetDisabled(id)) return;
        if (!_widgetHasState(id, FOCUSED)) return;

        const state = _generateFullState(id, value);

        _applyImageAndTextJson(id, state);
        _updateElementIdImageStyle(id, state);
    }

    $ax.style.SetWidgetSelected = function(id, value, alwaysApply) {
        if(_isWidgetDisabled(id)) return;
        //NOTE: not firing select events if state didn't change
        const raiseSelectedEvents = $ax.style.IsWidgetSelected(id) != value;

        if(value) {
            var group = $('#' + id).attr('selectiongroup');
            if(group) {
                $("[selectiongroup='" + group + "']").each(function() {
                    var otherId = this.id;
                    if(otherId == id) return;
                    if ($ax.visibility.isScriptIdLimbo($ax.repeater.getScriptIdFromElementId(otherId))) return;
                    $ax.style.SetWidgetSelected(otherId, false, alwaysApply);
                });
            }
        }
        var obj = $obj(id);
        if(obj) {
            // Lets remove 'selected' css class independently of object type (dynamic panel, layer or simple rectangle). See RP-1559
            if (!value) $jobj(id).removeClass(SELECTED);

            var actionId = id;
            if ($ax.public.fn.IsDynamicPanel(obj.type) || $ax.public.fn.IsLayerOrRdo(obj.type)) {
                var children = $axure('#' + id).getChildren()[0].children;
                var skipIds = new Set();
                for(var i = 0; i < children.length; i++) {
                    var childId = children[i];
                    // only set one member of selection group in children selected since subsequent calls
                    // will unselect the previous one anyway
                    if(value) {
                        if(skipIds.has(childId)) continue;
                        var group = $('#' + childId).attr('selectiongroup');
                        if(group) for (var item of $("[selectiongroup='" + group + "']")) skipIds.add(item.id);
                    }
                    // Special case for trees
                    var childObj = $jobj(childId);
                    if(childObj.hasClass('treeroot')) {
                        var treenodes = childObj.find('.treenode');
                        for(var j = 0; j < treenodes.length; j++) {
                            $axure('#' + treenodes[j].id).selected(value);
                        }
                    } else $axure('#' + childId).selected(value);
                }
            } else {
                const widgetHasSelectedState = _widgetHasState(id, SELECTED);
                while(obj.isContained && !widgetHasSelectedState) obj = obj.parent;
                var itemId = $ax.repeater.getItemIdFromElementId(id);
                var path = $ax.getPathFromScriptId($ax.repeater.getScriptIdFromElementId(id));
                path[path.length - 1] = obj.id;
                actionId = $ax.getElementIdFromPath(path, { itemNum: itemId });
                if(alwaysApply || widgetHasSelectedState) {
                    const state = _generateFullState(actionId, undefined, value);
                    _applyImageAndTextJson(actionId, state);
                    _updateElementIdImageStyle(actionId, state);
                }
                //added actionId and this hacky logic because we set style state on child, but interaction on parent
                //then the id saved in _selectedWidgets would be depended on widgetHasSelectedState... more see case 1818143
                while(obj.isContained && !$ax.getObjectFromElementId(id).interactionMap) obj = obj.parent;
                path = $ax.getPathFromScriptId($ax.repeater.getScriptIdFromElementId(id));
                path[path.length - 1] = obj.id;
                actionId = $ax.getElementIdFromPath(path, { itemNum: itemId });
            }
        }

        //    ApplyImageAndTextJson(id, value ? 'selected' : 'normal');
        _selectedWidgets[id] = value;
        if(raiseSelectedEvents) $ax.event.raiseSelectedEvents(actionId, value);
    };

    $ax.style.IsWidgetSelected = function(id) {
        return Boolean(_selectedWidgets[id] || _hasAnySelectedClass(id));
    };

    $ax.style.SetWidgetEnabled = function(id, value) {
        _disabledWidgets[id] = !value;
        $('#' + id).find('a').css('cursor', value ? 'pointer' : 'default');

        if(!_widgetHasState(id, DISABLED)) return;
        if(!value) {
            const state = _generateFullState(id, undefined, undefined, true);
            _applyImageAndTextJson(id, state);
            _updateElementIdImageStyle(id, state);
        } else $ax.style.SetWidgetSelected(id, $ax.style.IsWidgetSelected(id), true);
    };

    $ax.style.IsWidgetError = function (id) {
        return Boolean(_errorWidgets[id] || _hasAnyErrorClass(id));
    };

    $ax.style.SetWidgetError = function (id, value) {
        if(_isWidgetDisabled(id)) return;

        var raiseErrorEvents = $ax.style.IsWidgetError(id) != value;
        _errorWidgets[id] = value;
        if(raiseErrorEvents) $ax.event.raiseErrorEvents(id, value);
        if(!_widgetHasState(id, ERROR) && !_hasAnyErrorClass(id)) return;
        const state = _generateFullState(id, undefined, undefined, undefined, value);
        _applyImageAndTextJson(id, state);
        _updateElementIdImageStyle(id, state);
    }

    $ax.style.ShowingHint = function(id) {
        return _hintWidgets[id] === true;
    };

    $ax.style.SetWidgetPlaceholder = function(id, active, text, password) {
        _hintWidgets[id] = active && text && text.length > 0;

        const state = _generateState(id);
        const inputId = $ax.repeater.applySuffixToElementId(id, '_input');
        const obj = $jobj(inputId);

        if(!active) {
            try { //ie8 and below error
                if(password) document.getElementById(inputId).type = 'password';
            } catch(e) { } 
        } else {
            try { //ie8 and below error
                if(password && text) document.getElementById(inputId).type = 'text';
            } catch(e) { }
        }

        obj.val(text);

        const style = _computeAllOverrides(id, undefined, state, $ax.adaptive.currentViewId);
        if(!$.isEmptyObject(style)) _applyTextStyle(inputId, style);

        _updateStateClasses(
            [
                id,
                $ax.repeater.applySuffixToElementId(id, '_div'),
                inputId
            ], state, false
        );
    };

    var _isWidgetDisabled = $ax.style.IsWidgetDisabled = function(id) {
        return Boolean(_disabledWidgets[id]);
    };

    var _elementIdsToImageOverrides = {};
    $ax.style.mapElementIdToImageOverrides = function (elementId, override) {
        for(var key in override) _addImageOverride(elementId, key, override[key]);
    };

    var _addImageOverride = function (elementId, state, val) {
        if (!_elementIdsToImageOverrides[elementId]) _elementIdsToImageOverrides[elementId] = {};
        _elementIdsToImageOverrides[elementId][state] = val;
    }

    $ax.style.deleteElementIdToImageOverride = function(elementId) {
        delete _elementIdsToImageOverrides[elementId];
    };

    $ax.style.getElementImageOverride = function(elementId, state) {
        var url = _elementIdsToImageOverrides[elementId] && _elementIdsToImageOverrides[elementId][state];
        return url;
    };

    $ax.style.elementHasAnyImageOverride = function(elementId) {
        return Boolean(_elementIdsToImageOverrides[elementId]);
    };

    // these camel case state names match up to generated
    // javascript properties such as keys for compound state images

    var NORMAL = 'normal';
    var MOUSE_OVER = 'mouseOver';
    var MOUSE_DOWN = 'mouseDown';
    var SELECTED = 'selected';
    var DISABLED = 'disabled';
    var ERROR = 'error';
    var HINT = 'hint';
    var FOCUSED = 'focused';

    var SELECTED_ERROR = 'selectedError';
    var SELECTED_HINT = 'selectedHint';
    var SELECTED_ERROR_HINT = 'selectedErrorHint';
    var ERROR_HINT = 'errorHint';

    var MOUSE_OVER_MOUSE_DOWN = 'mouseOverMouseDown';
    var MOUSE_OVER_SELECTED = 'mouseOverSelected';
    var MOUSE_OVER_ERROR = 'mouseOverError';
    var MOUSE_OVER_HINT = 'mouseOverHint';

    var MOUSE_OVER_SELECTED_ERROR = 'mouseOverSelectedError';
    var MOUSE_OVER_SELECTED_HINT = 'mouseOverSelectedHint';
    var MOUSE_OVER_SELECTED_ERROR_HINT = 'mouseOverSelectedErrorHint';
    var MOUSE_OVER_ERROR_HINT = 'mouseOverErrorHint';

    var MOUSE_DOWN_SELECTED = 'mouseDownSelected';
    var MOUSE_DOWN_ERROR = 'mouseDownError';
    var MOUSE_DOWN_HINT = 'mouseDownHint';

    var MOUSE_DOWN_SELECTED_ERROR = 'mouseDownSelectedError';
    var MOUSE_DOWN_SELECTED_HINT = 'mouseDownSelectedHint';
    var MOUSE_DOWN_SELECTED_ERROR_HINT = 'mouseDownSelectedErrorHint';
    var MOUSE_DOWN_ERROR_HINT = 'mouseDownErrorHint';

    var MOUSE_OVER_MOUSE_DOWN_SELECTED = 'mouseOverMouseDownSelected';
    var MOUSE_OVER_MOUSE_DOWN_ERROR = 'mouseOverMouseDownError';
    var MOUSE_OVER_MOUSE_DOWN_HINT = 'MouseOverMouseDownHint';

    var MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR = 'mouseOverMouseDownSelectedError';
    var MOUSE_OVER_MOUSE_DOWN_SELECTED_HINT = 'mouseOverMouseDownSelectedHint';
    var MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR_HINT = 'mouseOverMouseDownSelectedErrorHint';
    var MOUSE_OVER_MOUSE_DOWN_ERROR_HINT = 'mouseOverMouseDownErrorHint';

    var FOCUSED_SELECTED = 'focusedSelected';
    var FOCUSED_ERROR = 'focusedError';
    var FOCUSED_HINT = 'focusedHint';

    var FOCUSED_SELECTED_ERROR = 'focusedSelectedError';
    var FOCUSED_SELECTED_HINT = 'focusedSelectedHint';
    var FOCUSED_SELECTED_ERROR_HINT = 'focusedSelectedErrorHint';
    var FOCUSED_ERROR_HINT = 'focusedErrorHint';

    var HINT_DISABLED = 'hintDisabled';
    var SELECTED_DISABLED = 'selectedDisabled';
    var ERROR_DISABLED = 'errorDisabled';
    var SELECTED_HINT_DISABLED = 'selectedHintDisabled';
    var ERROR_HINT_DISABLED = 'errorHintDisabled';
    var SELECTED_ERROR_DISABLED = 'selectedErrorDisabled';
    var SELECTED_ERROR_HINT_DISABLED = 'selectedErrorHintDisabled';

    // Compound states are applied with multiple classes
    // #u0.mouseOver.mouseDown.Selected.Error in .css
    // class="mouseOver mouseDown selected error" in .html
    var ALL_STATES_WITH_CSS_CLASS = [
        MOUSE_OVER,
        MOUSE_DOWN,
        SELECTED,
        DISABLED,
        ERROR,
        HINT,
        FOCUSED
    ];

    var _stateHasFocused = function (state) {
        return state == FOCUSED
            || state == FOCUSED_SELECTED
            || state == FOCUSED_ERROR
            || state == FOCUSED_HINT
            || state == FOCUSED_SELECTED_ERROR
            || state == FOCUSED_SELECTED_HINT
            || state == FOCUSED_SELECTED_ERROR_HINT
            || state == FOCUSED_ERROR_HINT;
    };

    var _hasAnyFocusedClass = _style.HasAnyFocusedClass = function (id) {
        const jobj = $('#' + id);
        return jobj.hasClass(FOCUSED);
    };

    var _stateHasHint = function (state) {
        return state == HINT
            || state == SELECTED_HINT
            || state == SELECTED_ERROR_HINT
            || state == ERROR_HINT
            || state == MOUSE_OVER_HINT
            || state == MOUSE_OVER_SELECTED_HINT
            || state == MOUSE_OVER_SELECTED_ERROR_HINT
            || state == MOUSE_OVER_ERROR_HINT
            || state == MOUSE_DOWN_HINT
            || state == MOUSE_DOWN_SELECTED_HINT
            || state == MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == MOUSE_DOWN_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_ERROR_HINT
            || state == FOCUSED_HINT
            || state == FOCUSED_SELECTED_HINT
            || state == FOCUSED_SELECTED_ERROR_HINT
            || state == FOCUSED_ERROR_HINT
            || state == HINT_DISABLED
            || state == SELECTED_HINT_DISABLED
            || state == ERROR_HINT_DISABLED
            || state == SELECTED_ERROR_HINT_DISABLED;
    };

    var _stateHasError = function (state) {
        return state == ERROR
            || state == SELECTED_ERROR
            || state == SELECTED_ERROR_HINT
            || state == ERROR_HINT
            || state == MOUSE_OVER_ERROR
            || state == MOUSE_OVER_SELECTED_ERROR
            || state == MOUSE_OVER_SELECTED_ERROR_HINT
            || state == MOUSE_OVER_ERROR_HINT
            || state == MOUSE_DOWN_ERROR
            || state == MOUSE_DOWN_SELECTED_ERROR
            || state == MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == MOUSE_DOWN_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_ERROR
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_ERROR_HINT
            || state == FOCUSED_ERROR
            || state == FOCUSED_SELECTED_ERROR
            || state == FOCUSED_SELECTED_ERROR_HINT
            || state == FOCUSED_ERROR_HINT
            || state == ERROR_DISABLED
            || state == ERROR_HINT_DISABLED
            || state == SELECTED_ERROR_DISABLED
            || state == SELECTED_ERROR_HINT_DISABLED;
    };

    var _hasAnyErrorClass = _style.HasAnyErrorClass = function (id) {
        const jobj = $('#' + id);
        return jobj.hasClass(ERROR);
    };

    var _stateHasDisabled = _style.StateHasDisabled = function (state) {
        return state == DISABLED
            || state == HINT_DISABLED
            || state == SELECTED_DISABLED
            || state == ERROR_DISABLED
            || state == SELECTED_HINT_DISABLED
            || state == ERROR_HINT_DISABLED
            || state == SELECTED_ERROR_DISABLED
            || state == SELECTED_ERROR_HINT_DISABLED;
    };

    var _hasAnyDisabledClass = _style.HasAnyDisabledClass = function (id) {
        const jobj = $('#' + id);
        return jobj.hasClass(DISABLED);
    };

    var _stateHasSelected = _style.StateHasSelected = function (state) {
        return state == SELECTED
            || state == SELECTED_ERROR
            || state == SELECTED_HINT
            || state == SELECTED_ERROR_HINT
            || state == MOUSE_OVER_SELECTED
            || state == MOUSE_OVER_SELECTED_ERROR
            || state == MOUSE_OVER_SELECTED_HINT
            || state == MOUSE_OVER_SELECTED_ERROR_HINT
            || state == MOUSE_DOWN_SELECTED
            || state == MOUSE_DOWN_SELECTED_ERROR
            || state == MOUSE_DOWN_SELECTED_HINT
            || state == MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == FOCUSED_SELECTED
            || state == FOCUSED_SELECTED_ERROR
            || state == FOCUSED_SELECTED_HINT
            || state == FOCUSED_SELECTED_ERROR_HINT
            || state == SELECTED_DISABLED
            || state == SELECTED_HINT_DISABLED
            || state == SELECTED_ERROR_DISABLED
            || state == SELECTED_ERROR_HINT_DISABLED;
    };

    var _hasAnySelectedClass = _style.HasAnySelectedClass = function (id) {
        const jobj = $('#' + id);
        return jobj.hasClass(SELECTED);
    };

    var _removeAnySelectedClass = _style.RemoveAnySelectedClass = function (id) {
        const jobj = $('#' + id);
        jobj.removeClass(SELECTED);
    };

    var _stateHasMouseOver = function (state) {
        return state == MOUSE_OVER
            || state == MOUSE_OVER_MOUSE_DOWN
            || state == MOUSE_OVER_SELECTED
            || state == MOUSE_OVER_ERROR
            || state == MOUSE_OVER_HINT
            || state == MOUSE_OVER_SELECTED_ERROR
            || state == MOUSE_OVER_SELECTED_HINT
            || state == MOUSE_OVER_SELECTED_ERROR_HINT
            || state == MOUSE_OVER_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED
            || state == MOUSE_OVER_MOUSE_DOWN_ERROR
            || state == MOUSE_OVER_MOUSE_DOWN_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_ERROR_HINT;
    };

    var _stateHasMouseDown = function (state) {
        return state == MOUSE_DOWN
            || state == MOUSE_OVER_MOUSE_DOWN
            || state == MOUSE_DOWN_SELECTED
            || state == MOUSE_DOWN_ERROR
            || state == MOUSE_DOWN_HINT
            || state == MOUSE_DOWN_SELECTED_ERROR
            || state == MOUSE_DOWN_SELECTED_HINT
            || state == MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == MOUSE_DOWN_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED
            || state == MOUSE_OVER_MOUSE_DOWN_ERROR
            || state == MOUSE_OVER_MOUSE_DOWN_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR_HINT
            || state == MOUSE_OVER_MOUSE_DOWN_ERROR_HINT;
    };

    var _generateState = _style.generateState = function (id) {
        return _generateFullState(id);
    };

    // we need this for when we are looking for the correct image to apply
    // it tells us what image override to try to apply if it exists
    var _highestPriorityBaseState = _style.highestPriorityBaseState = function (state) {
        if (state == SELECTED_ERROR) return ERROR;
        if (state == SELECTED_HINT) return HINT;
        if (state == SELECTED_ERROR_HINT) return HINT;
        if (state == ERROR_HINT) return HINT;

        if (state == MOUSE_OVER_MOUSE_DOWN) return MOUSE_DOWN;
        if (state == MOUSE_OVER_SELECTED) return SELECTED;
        if (state == MOUSE_OVER_ERROR) return ERROR;
        if (state == MOUSE_OVER_HINT) return HINT;

        if (state == MOUSE_OVER_SELECTED_ERROR) return ERROR;
        if (state == MOUSE_OVER_SELECTED_HINT) return HINT;
        if (state == MOUSE_OVER_SELECTED_ERROR_HINT) return HINT;
        if (state == MOUSE_OVER_ERROR_HINT) return HINT;

        if (state == MOUSE_DOWN_SELECTED) return SELECTED;
        if (state == MOUSE_DOWN_ERROR) return ERROR;
        if (state == MOUSE_DOWN_HINT) return HINT;

        if (state == MOUSE_DOWN_SELECTED_ERROR) return ERROR;
        if (state == MOUSE_DOWN_SELECTED_HINT) return HINT;
        if (state == MOUSE_DOWN_SELECTED_ERROR_HINT) return HINT;
        if (state == MOUSE_DOWN_ERROR_HINT) return HINT;

        if (state == MOUSE_OVER_MOUSE_DOWN_SELECTED) return SELECTED;
        if (state == MOUSE_OVER_MOUSE_DOWN_ERROR) return ERROR;
        if (state == MOUSE_OVER_MOUSE_DOWN_HINT) return HINT;

        if (state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR) return ERROR;
        if (state == MOUSE_OVER_MOUSE_DOWN_SELECTED_HINT) return HINT;
        if (state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR_HINT) return HINT;
        if (state == MOUSE_OVER_MOUSE_DOWN_ERROR_HINT) return HINT;

        if (state == FOCUSED_SELECTED) return SELECTED;
        if (state == FOCUSED_ERROR) return ERROR;
        if (state == FOCUSED_HINT) return HINT;

        if (state == FOCUSED_SELECTED_ERROR) return ERROR;
        if (state == FOCUSED_SELECTED_HINT) return HINT;
        if (state == FOCUSED_SELECTED_ERROR_HINT) return HINT;
        if (state == FOCUSED_ERROR_HINT) return HINT;

        if (state == HINT_DISABLED) return DISABLED;
        if (state == SELECTED_DISABLED) return DISABLED;
        if (state == ERROR_DISABLED) return DISABLED;
        if (state == SELECTED_HINT_DISABLED) return DISABLED;
        if (state == ERROR_HINT_DISABLED) return DISABLED;
        if (state == SELECTED_ERROR_DISABLED) return DISABLED;
        if (state == SELECTED_ERROR_HINT_DISABLED) return DISABLED;

        return state;
    };

    // we need this for when we are looking for the correct image to apply
    var _decomposeState = _style.decomposeState = function(state) {
        if(state == NORMAL) return false;
        if(state == SELECTED_ERROR) return SELECTED;
        if(state == SELECTED_HINT) return SELECTED;
        if(state == SELECTED_ERROR_HINT) return SELECTED_ERROR;
        if(state == ERROR_HINT) return ERROR;

        if(state == MOUSE_OVER_MOUSE_DOWN) return MOUSE_OVER;
        if(state == MOUSE_OVER_SELECTED) return MOUSE_OVER;
        if(state == MOUSE_OVER_ERROR) return MOUSE_OVER;
        if(state == MOUSE_OVER_HINT) return MOUSE_OVER;

        if(state == MOUSE_OVER_SELECTED_ERROR) return MOUSE_OVER_SELECTED;
        if(state == MOUSE_OVER_SELECTED_HINT) return MOUSE_OVER_SELECTED;
        if(state == MOUSE_OVER_SELECTED_ERROR_HINT) return MOUSE_OVER_SELECTED_ERROR;
        if(state == MOUSE_OVER_ERROR_HINT) return MOUSE_OVER_ERROR;

        if(state == MOUSE_DOWN_SELECTED) return MOUSE_DOWN;
        if(state == MOUSE_DOWN_ERROR) return MOUSE_DOWN;
        if(state == MOUSE_DOWN_HINT) return MOUSE_DOWN;

        if(state == MOUSE_DOWN_SELECTED_ERROR) return MOUSE_DOWN_SELECTED;
        if(state == MOUSE_DOWN_SELECTED_HINT) return MOUSE_DOWN_SELECTED;
        if(state == MOUSE_DOWN_SELECTED_ERROR_HINT) return MOUSE_DOWN_SELECTED_ERROR;
        if(state == MOUSE_DOWN_ERROR_HINT) return MOUSE_DOWN_ERROR;

        if(state == MOUSE_OVER_MOUSE_DOWN_SELECTED) return MOUSE_OVER_MOUSE_DOWN;
        if(state == MOUSE_OVER_MOUSE_DOWN_ERROR) return MOUSE_OVER_MOUSE_DOWN;
        if(state == MOUSE_OVER_MOUSE_DOWN_HINT) return MOUSE_OVER_MOUSE_DOWN;

        if(state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR) return MOUSE_OVER_MOUSE_DOWN_SELECTED;
        if(state == MOUSE_OVER_MOUSE_DOWN_SELECTED_HINT) return MOUSE_OVER_MOUSE_DOWN_SELECTED;
        if(state == MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR_HINT) return MOUSE_OVER_MOUSE_DOWN_SELECTED_ERROR;
        if(state == MOUSE_OVER_MOUSE_DOWN_ERROR_HINT) return MOUSE_OVER_MOUSE_DOWN_ERROR;

        if(state == FOCUSED_SELECTED) return FOCUSED;
        if(state == FOCUSED_ERROR) return FOCUSED;
        if(state == FOCUSED_HINT) return FOCUSED;

        if(state == FOCUSED_SELECTED_ERROR) return FOCUSED_SELECTED;
        if(state == FOCUSED_SELECTED_HINT) return FOCUSED_SELECTED;
        if(state == FOCUSED_SELECTED_ERROR_HINT) return FOCUSED_SELECTED_ERROR;
        if(state == FOCUSED_ERROR_HINT) return FOCUSED_ERROR;

        if (state == HINT_DISABLED) return HINT;
        if (state == SELECTED_DISABLED) return SELECTED;
        if (state == ERROR_DISABLED) return ERROR;
        if (state == SELECTED_HINT_DISABLED) return SELECTED_HINT;
        if (state == ERROR_HINT_DISABLED) return ERROR_HINT;
        if (state == SELECTED_ERROR_DISABLED) return SELECTED_ERROR;
        if (state == SELECTED_ERROR_HINT_DISABLED) return SELECTED_ERROR_HINT;

        return NORMAL;
    };

    var _updateElementIdImageStyle = _style.updateElementIdImageStyle = function(elementId, state) {
        if(!_style.elementHasAnyImageOverride(elementId)) return;

        if(!state) state = _generateState(elementId);

        var style = _computeFullStyle(elementId, state, $ax.adaptive.currentViewId);

        var query = $jobj($ax.repeater.applySuffixToElementId(elementId, '_img'));
        style.size.width = query.width();
        style.size.height = query.height();
        var borderId = $ax.repeater.applySuffixToElementId(elementId, '_border');
        var borderQuery = $jobj(borderId);
        if(!borderQuery.length) {
            borderQuery = $('<div></div>');
            borderQuery.attr('id', borderId);
            query.after(borderQuery);
        }

        borderQuery.attr('style', '');
        //borderQuery.css('position', 'absolute');
        query.attr('style', '');

        var borderQueryCss = { 'position': 'absolute' };
        var queryCss = {}

        var borderWidth = Number(style.borderWidth);
        var hasBorderWidth = borderWidth > 0;
        if(hasBorderWidth) {
            //borderQuery.css('border-style', 'solid');
            //borderQuery.css('border-width', borderWidth + 'px'); // If images start being able to turn off borders on specific sides, need to update this.
            //borderQuery.css('width', style.size.width - borderWidth * 2);
            //borderQuery.css('height', style.size.height - borderWidth * 2);
            //borderQuery.css({
            //    'border-style': 'solid',
            //    'border-width': borderWidth + 'px',
            //    'width': style.size.width - borderWidth * 2,
            //    'height': style.size.height - borderWidth * 2
            //});
            borderQueryCss['border-style'] = 'solid';
            borderQueryCss['border-width'] = borderWidth + 'px'; // If images start being able to turn off borders on specific sides, need to update this.
            borderQueryCss['width'] = style.size.width - borderWidth * 2;
            borderQueryCss['height'] = style.size.height - borderWidth * 2;
        }

        var linePattern = style.linePattern;
        if(hasBorderWidth && linePattern) borderQueryCss['border-style'] = linePattern;

        var borderFill = style.borderFill;
        if(hasBorderWidth && borderFill) {
            var color = borderFill.fillType == 'solid' ? borderFill.color :
                borderFill.fillType == 'linearGradient' ? borderFill.colors[0].color : 0;

            var alpha = Math.floor(color / 256 / 256 / 256);
            color -= alpha * 256 * 256 * 256;
            alpha = alpha / 255;

            var red = Math.floor(color / 256 / 256);
            color -= red * 256 * 256;
            var green = Math.floor(color / 256);
            var blue = color - green * 256;

            borderQueryCss['border-color'] = _rgbaToFunc(red, green, blue, alpha);
        }

        var cornerRadiusTopLeft = style.cornerRadius;
        if(cornerRadiusTopLeft) {
            queryCss['border-radius'] = cornerRadiusTopLeft + 'px';
            borderQueryCss['border-radius'] = cornerRadiusTopLeft + 'px';
        }

        var outerShadow = style.outerShadow;
        if(outerShadow && outerShadow.on) {
            var arg = '';
            arg += outerShadow.offsetX + 'px' + ' ' + outerShadow.offsetY + 'px' + ' ';
            var rgba = outerShadow.color;
            arg += outerShadow.blurRadius + 'px' + ' 0px ' + _rgbaToFunc(rgba.r, rgba.g, rgba.b, rgba.a);
            //query.css('-moz-box-shadow', arg);
            //query.css('-wibkit-box-shadow', arg);
            //query.css('box-shadow', arg);
            //query.css('left', '0px');
            //query.css('top', '0px');
            //query.css({
            //    '-moz-box-shadow': arg,
            //    '-webkit-box-shadow': arg,
            //    'box-shadow': arg,
            //    'left': '0px',
            //    'top': '0px'
            //});
            queryCss['-moz-box-shadow'] = arg;
            queryCss['-wibkit-box-shadow'] = arg;
            queryCss['box-shadow'] = arg;
            queryCss['left'] = '0px';
            queryCss['top'] = '0px';
        }

        queryCss['width'] = style.size.width;
        queryCss['height'] = style.size.height;

        borderQuery.css(borderQueryCss);
        query.css(queryCss);

        //query.css({ width: style.size.width, height: style.size.height });
    };

    var _rgbaToFunc = function(red, green, blue, alpha) {
        return 'rgba(' + red + ',' + green + ',' + blue + ',' + alpha + ')';
    };

    var _enableStateTransitions = function () {
        $('body').removeClass('notransition');
    }
    $ax.style.enableStateTransitions = _enableStateTransitions;
    var _disableStateTransitions = function () {
        $('body').addClass('notransition');
    }
    $ax.style.disableStateTransitions = _disableStateTransitions;

    var _idToAppliedStyles = {};
    $ax.style.isLastAppliedStyle = function (id, className) {
        if (_idToAppliedStyles[id] && _idToAppliedStyles[id][_idToAppliedStyles[id].length - 1] === className) return true;
        return false;
    }

    $ax.style.setApplyStyleTag = function (className, cssRule) {
        var head = document.getElementsByTagName('head')[0];
        var styleTag = head.querySelector('#' + className);
        if (!styleTag) {
            styleTag = document.createElement('style');
            styleTag.type = 'text/css';
            styleTag.id = className;
            styleTag.innerHTML = cssRule;
        }
        head.appendChild(styleTag);
    }

    $ax.style.applyWidgetStyle = function (id, className, style, image, clearPrevious) {
        if (!clearPrevious && $ax.style.isLastAppliedStyle(id, className)) return;

        _enableStateTransitions();
        if (clearPrevious && _idToAppliedStyles[id] && _idToAppliedStyles[id].classList) {
            for (var i = 0; i < _idToAppliedStyles[id].classList.length; i++) {
                var classNameToRemove = _idToAppliedStyles[id].classList[i];
                $jobj(id).removeClass(classNameToRemove);
                $jobj($ax.repeater.applySuffixToElementId(id, '_div')).removeClass(classNameToRemove);
                $jobj($ax.repeater.applySuffixToElementId(id, '_text')).removeClass(classNameToRemove);
                $jobj($ax.repeater.applySuffixToElementId(id, '_input')).removeClass(classNameToRemove);
            }
            _idToAppliedStyles[id] = {
                classList: [],
                style: {},
            };
        }

        if (_idToAppliedStyles[id] && _idToAppliedStyles[id].classList && _idToAppliedStyles[id].style) {
            var index = _idToAppliedStyles[id].classList.indexOf(className);
            if (index > -1) _idToAppliedStyles[id].classList.splice(index, 1);
            _idToAppliedStyles[id].classList.push(className);
            _idToAppliedStyles[id].style = $.extend({}, _idToAppliedStyles[id].style, style);
        } else {
            _idToAppliedStyles[id] = {
                classList: [className],
                style: style,
            };
        }

        $jobj(id).removeClass(className);
        $jobj($ax.repeater.applySuffixToElementId(id, '_div')).removeClass(className);
        $jobj($ax.repeater.applySuffixToElementId(id, '_text')).removeClass(className);
        $jobj($ax.repeater.applySuffixToElementId(id, '_input')).removeClass(className);

        $jobj(id).addClass(className);
        $jobj($ax.repeater.applySuffixToElementId(id, '_div')).addClass(className);
        $jobj($ax.repeater.applySuffixToElementId(id, '_text')).addClass(className);
        $jobj($ax.repeater.applySuffixToElementId(id, '_input')).addClass(className);

        if ($ax.public.fn.IsImageBox($obj(id).type)) {
            if (image) {
                _applyImage(id, image, "interaction");
            }
        } else {
            _applySvg(id, "interaction", $.extend(_computeFullStyle(id, "interaction", $ax.adaptive.currentViewId), _idToAppliedStyles[id].style));
        }
    }

    $axure.messageCenter.addMessageListener(function (message, data) {
        if (message == "switchAdaptiveView") {
            var ids = Object.keys(_idToAppliedStyles);
            for (var i = 0; i < ids.length; i++) {
                var id = ids[i];
                for (var j = 0; j < _idToAppliedStyles[id].length; j++) {
                    var className = _idToAppliedStyles[id][j];
                    $jobj(id).removeClass(className);
                    $jobj($ax.repeater.applySuffixToElementId(id, '_div')).removeClass(className);
                    $jobj($ax.repeater.applySuffixToElementId(id, '_text')).removeClass(className);
                    $jobj($ax.repeater.applySuffixToElementId(id, '_input')).removeClass(className);
                }
            }
            _idToAppliedStyles = {};
        }
    });

    var _applySvg = function (id, event, style) {

        let overridedStyle = style;
        if (!overridedStyle) {
            overridedStyle = _computeFullStyle(id, event, $ax.adaptive.currentViewId);
            if (_idToAppliedStyles[id] && _idToAppliedStyles[id].style) {
                $.extend(overridedStyle, _idToAppliedStyles[id].style);
            }
        }
        const originalBorderWidth = _computeFullStyle(id, "normal", $ax.adaptive.currentViewId).borderWidth;
        const transition = "transition:" + $jobj(id).css('transition') + ", d 0s" + ";";
        if(!$.isEmptyObject(overridedStyle)) {

            //TODO
            //Background Image
            //Maybe change SVG gen to use filter for Inner shadows instead of image
            //Radial Gradients
            //Correct Dash Array and Offset
            //compound is currently generating more image files than needed - audit other places
            //Line ends on compound
            //Image widgets than gen as SVG due to props

            function applyStyleOverrides(svgId, overridedStyle) {
                //if(!contentDoc) return;
                var svgns = 'http://www.w3.org/2000/svg';              
                //var svgTag = contentDoc.getElementsByTagName("svg")[0];
                var svgTag = document.getElementById(svgId);
                var style = svgTag.getElementsByTagName("style")[0];
                var viewBox = svgTag.getAttribute("viewBox");
                var viewBoxValues = viewBox && viewBox.split(" ");
                var viewBoxX = parseFloat(viewBoxValues && viewBoxValues[0]) || 0;
                var viewBoxY = parseFloat(viewBoxValues && viewBoxValues[1]) || 0;
                if(!style) {
                    style = document.createElementNS(svgns, "style");
                    svgTag.appendChild(style);
                }
                var styleHtml = "";
                setFill();
                setBorder();
                setOuterShadow();
                setInnerShadow();
                setBackgroundImage();
                

                style.innerHTML = styleHtml;
                function setBackgroundImage() {
                    if (overridedStyle.image) {
                        var defsTag = svgTag.getElementsByTagName("defs")[0];
                        if (!defsTag) {
                            defsTag = document.createElementNS(svgns, "defs");
                            svgTag.appendChild(defsTag);
                        }
                        var bgPatternId = "#" + svgId + "_bgp";
                        var pattern = defsTag.querySelector(bgPatternId);
                        var image = pattern.querySelector("image");
                        if (!image) {
                            image = document.createElementNS(svgns, "image");
                            image.setAttribute("preserveAspectRatio", "none");
                            pattern.appendChild(image);

                            var fillElement = svgTag.querySelector(".fill");
                            var backgroundElement = fillElement.cloneNode();
                            backgroundElement.setAttribute("class", "background-image");
                            backgroundElement.setAttribute("fill", "url(" + bgPatternId + ")");
                            fillElement.after(backgroundElement);
                        }
                        image.setAttribute("href", overridedStyle.image.path);

                        var patternRect = {
                            x: 0,
                            y: 0,
                            width: overridedStyle.size.width,
                            height: overridedStyle.size.height,
                        };
                        var imageRect = {
                            x: 0,
                            y: 0,
                            width: overridedStyle.image.width,
                            height: overridedStyle.image.height,
                        };
                        var alignment = pattern.getAttribute("alignment").split(" ");
                        var horizontalAlignment = alignment[0];
                        var verticalAlignment = alignment[1];
                        var repeat = pattern.getAttribute("imagerepeat");

                        if (repeat === "Repeat" || repeat === "RepeatX" || repeat === "RepeatY" || repeat === "None") {
                            if (horizontalAlignment == 2) imageRect.x = patternRect.width - imageRect.width;
                            if (horizontalAlignment == 1) imageRect.x = (patternRect.width - imageRect.width) / 2;
                            if (verticalAlignment == 2) imageRect.y = patternRect.height - imageRect.height;
                            if (verticalAlignment == 1) imageRect.y = (patternRect.height - imageRect.height) / 2;
                        }
                        if (repeat === "Repeat" || repeat === "RepeatX") {
                            patternRect.width = imageRect.width;
                        }
                        if (repeat === "Repeat" || repeat === "RepeatY") {
                            patternRect.height = imageRect.height;
                        }
                        if (repeat === "None") {
                            if (imageRect.height > patternRect.height) patternRect.height = imageRect.height;
                            if (patternRect.width > patternRect.width) patternRect.hidth = imageRect.width;
                        }
                        if (repeat === "StretchCover") {
                            var wRatio = patternRect.width / imageRect.width;
                            var hRatio = patternRect.height / imageRect.height;
                            var ratio = wRatio;
                            if (hRatio > wRatio) ratio = hRatio;

                            var newWidth = imageRect.width * ratio;
                            var newHeight = imageRect.height * ratio;

                            var left = 0;
                            if (newWidth > patternRect.width) {
                                if (horizontalAlignment == 1) left = (patternRect.width - newWidth) / 2;
                                else if (horizontalAlignment == 2) left = patternRect.width - newWidth;
                            }

                            var top = 0;
                            if(newHeight > patternRect.height) {
                                if (verticalAlignment == 1) top = (patternRect.height - newHeight) / 2;
                                else if (verticalAlignment == 2) top = patternRect.height - newHeight;
                            }

                            patternRect = imageRect = { x: patternRect.x + left, y: patternRect.y + top, width: newWidth, height: newHeight };
                        } else if (repeat === "StretchContain") {
                            var ratio = imageRect.width / imageRect.height;
                            var newWidth = patternRect.width;
                            var newHeight = patternRect.width / ratio;

                            if (newHeight > patternRect.height) {
                                newWidth = patternRect.height * ratio;
                                newHeight = patternRect.height;
                            }

                            var newRect = { x: 0, y: 0, width: newWidth, height: newHeight };
                            if (newWidth < patternRect.width) {
                                if (horizontalAlignment == 1) newRect.x = (patternRect.width - newWidth) / 2;
                                else if (horizontalAlignment == 2) newRect.x = patternRect.width - newWidth;
                            }

                            if (newHeight < patternRect.height) {
                                if (verticalAlignment == 1) newRect.Y = (patternRect.height - newHeight) / 2;
                                else if (verticalAlignment == 2) newRect.Y = patternRect.height - newHeight;
                            }
                            imageRect = newRect;
                        } else if (repeat === "Stretch") {
                            imageRect = patternRect;
                        }

                        image.setAttribute("width", imageRect.width);
                        image.setAttribute("height", imageRect.height);
                        pattern.setAttribute("patternTransform", "translate(" + imageRect.x + " " + imageRect.y + ")");
                        pattern.setAttribute("width", patternRect.width);
                        pattern.setAttribute("height", patternRect.height);
                    } else {
                        styleHtml += "#" + svgId + " .background-image { fill: rgba(0,0,0,0); } "
                    }
                }

                function setFill() {
                    var fillStyle = "";
                    if (overridedStyle.fill) {
                        var styleFill = overridedStyle.fill;
                        if (styleFill.fillType == "solid") {
                            var fillColor = _getColorFromFill(styleFill);
                            fillStyle += "fill:" + fillColor + "; ";
                        } else if (styleFill.fillType == "linearGradient") {
                            var gradientId = "id" + Math.random().toString(16).slice(2);
                            if (insertLinearGradient(gradientId, styleFill, "fill")) {
                                fillStyle += "fill: url(#" + gradientId + "); ";
                            }
                        } else if (styleFill.fillType == "radialGradient") {
                            var gradientId = "id" + Math.random().toString(16).slice(2);
                            if (insertRadialGradient(gradientId, styleFill, "fill")) {
                                fillStyle += "fill: url(#" + gradientId + "); ";
                            }
                        }

                        fillStyle += transition;
                    }

                    styleHtml += "#" + svgId + " .fill { " + fillStyle + " } "
                }

                function setBorder() {
                    var borderStyle = "";
                    var arrowHeadStyle = "";
                    if (overridedStyle.borderFill) {
                        var styleFill = overridedStyle.borderFill;
                        if (styleFill.fillType == "solid") {
                            var borderColor = _getColorFromFill(styleFill);
                            borderStyle += "stroke:" + borderColor + "; ";
                            arrowHeadStyle += "stroke:" + borderColor + "; fill:" + borderColor + "; ";
                        } else if (styleFill.fillType == "linearGradient") {
                            var gradientId = "id" + Math.random().toString(16).slice(2);
                            if (insertLinearGradient(gradientId, styleFill, "stroke")) {
                                borderStyle += "stroke: url(#" + gradientId + "); ";
                                arrowHeadStyle += "stroke: url(#" + gradientId + "); fill: url(#" + gradientId + "); ";
                            } else {
                                var arrowhead = svgTag.querySelector(".arrowhead");
                                if (arrowhead) {
                                    var fill = window.getComputedStyle(svgTag.querySelector(".stroke")).stroke;
                                    arrowhead.setAttribute("fill", fill);
                                    arrowhead.setAttribute("stroke", fill);
                                }
                            }
                        } else if (styleFill.fillType == "radialGradient") {
                            var gradientId = "id" + Math.random().toString(16).slice(2);
                            if (insertRadialGradient(gradientId, styleFill, "stroke")) {
                                borderStyle += "stroke: url(#" + gradientId + "); ";
                                arrowHeadStyle += "stroke: url(#" + gradientId + "); fill: url(#" + gradientId + "); ";
                            } else {
                                var arrowhead = svgTag.querySelector(".arrowhead");
                                if (arrowhead) {
                                    var fill = window.getComputedStyle(svgTag.querySelector(".stroke")).stroke;
                                    arrowhead.setAttribute("fill", fill);
                                    arrowhead.setAttribute("stroke", fill);
                                }
                            }
                        }
                    }
                    if (overridedStyle.linePattern == "none") {
                        borderStyle += "stroke-width: 0; stroke-dasharray: 0; ";
                        arrowHeadStyle = "fill: none";
                    } else {
                        if (overridedStyle.borderWidth) {
                            var closedAndClipped = true;
                            var newBorderWidth = overridedStyle.borderWidth;
                            var strokes = svgTag.getElementsByClassName("stroke");
                            var fills = svgTag.getElementsByClassName("fill");
                            if (obj.friendlyType === "Rectangle") {
                                var size = overridedStyle.size;
                                size = { width: Math.round(size.width), height: Math.round(size.height) };
                                var hasBorderTop = overridedStyle.borderVisibility.includes("top");
                                var hasBorderRight = overridedStyle.borderVisibility.includes("right");
                                var hasBorderBottom = overridedStyle.borderVisibility.includes("bottom");
                                var hasBorderLeft = overridedStyle.borderVisibility.includes("left");

                                var cornerRadius = Math.min(overridedStyle.cornerRadius, Math.min(size.width, size.height));
                                var hasCornerRadius = cornerRadius > 0;

                                var hasCornerTopRight = hasCornerRadius && overridedStyle.cornerVisibility.includes("top") && hasBorderTop == hasBorderRight;
                                var hasCornerBottomRight = hasCornerRadius && overridedStyle.cornerVisibility.includes("right") && hasBorderBottom == hasBorderRight;
                                var hasCornerBottomLeft = hasCornerRadius && overridedStyle.cornerVisibility.includes("bottom") && hasBorderBottom == hasBorderLeft;
                                var hasCornerTopLeft = hasCornerRadius && overridedStyle.cornerVisibility.includes("left") && hasBorderTop == hasBorderLeft;

                                var arcK = 0.44;
                                var halfWidth = newBorderWidth / 2;
                                var left = hasBorderLeft ? halfWidth : 0;
                                var top = hasBorderTop ? halfWidth : 0;
                                var right = size.width - (hasBorderRight ? halfWidth : 0);
                                var bottom = size.height - (hasBorderBottom ? halfWidth : 0);
                                var arc = (cornerRadius - halfWidth) * arcK + halfWidth;

                                var segments = [];
                                function fillTop(start) {
                                    if (hasBorderTop) {
                                        if (start && hasCornerTopLeft) segments.push("M " + cornerRadius + " " + top);
                                        else if (start || !hasBorderLeft) segments.push("M " + left + " " + top);

                                        if (hasCornerTopRight) {
                                            segments.push("L " + (size.width - cornerRadius) + " " + top);
                                            segments.push("C " + (size.width - arc) + " " + top + " " + right + " " + arc + " " + right + " " + cornerRadius);
                                        } else segments.push("L " + right + " " + top);
                                    }
                                    return fillRight;
                                }
                                function fillRight(start) {
                                    if (hasBorderRight) {
                                        if (start && hasCornerTopRight) segments.push("M " + right + " " + cornerRadius);
                                        else if (start || !hasBorderTop) segments.push("M " + right + " " + top);

                                        if (hasCornerBottomRight) {
                                            segments.push("L " + right + " " + (size.height - cornerRadius));
                                            segments.push("C " + right + " " + (size.height - arc) + " " + (size.width - arc) + " " + bottom + " " + (size.width - cornerRadius) + " " + bottom);
                                        } else segments.push("L " + right + " " + bottom);
                                    }
                                    return fillBottom;
                                }
                                function fillBottom(start) {
                                    if (hasBorderBottom) {                                        
                                        if (start && hasCornerBottomRight) segments.push("M " + right + " " + (size.height - cornerRadius));
                                        else if (start || !hasBorderRight) segments.push("M " + right + " " + bottom);

                                        if (hasCornerBottomLeft) {
                                            segments.push("L " + cornerRadius + " " + bottom);
                                            segments.push("C " + arc + " " + bottom + " " + left + " " + (size.height - arc) + " " + left + " " + (size.height - cornerRadius));
                                        } else segments.push("L " + left + " " + bottom);
                                    }
                                    return fillLeft;
                                }
                                function fillLeft(start) {
                                    if (hasBorderLeft) {
                                        if (start && hasCornerBottomLeft) segments.push("M " + left + " " + (size.height - cornerRadius));
                                        else if (start || !hasBorderBottom) segments.push("M " + left + " " + bottom);

                                        if (hasCornerTopLeft) {
                                            segments.push("L " + left + " " + cornerRadius);
                                            segments.push("C " + left + " " + arc + " " + arc + " " + top + " " + cornerRadius + " " + top);
                                        } else segments.push("L " + left + " " + top);
                                    }
                                    return fillTop;
                                }

                                if (!hasBorderTop) fillRight(true)(false)(false)(false);
                                else if (!hasBorderRight) fillBottom(true)(false)(false)(false);
                                else if (!hasBorderBottom) fillLeft(true)(false)(false)(false);
                                else if (!hasBorderLeft) fillTop(true)(false)(false)(false);
                                else {
                                    fillTop(true)(false)(false)(false);
                                    segments.push("Z");
                                }
                                var d = segments.join(" ");
                                for(var path of [...strokes]) {
                                    path.setAttribute("d", d);
                                    path.setAttribute("mask", "");
                                }

                                segments = [];
                                hasBorderTop = hasBorderRight = hasBorderBottom = hasBorderLeft = true;                                
                                left = 0;
                                top = 0;
                                right = size.width;
                                bottom = size.height;
                                arc = cornerRadius * arcK;

                                fillTop(true)(false)(false)(false);
                                segments.push("Z");
                                d = segments.join(" ");
                                for(var path of [ ...fills]) {
                                    path.setAttribute("d", d);
                                    path.setAttribute("mask", "");
                                }
                            } else {
                                for (var path of strokes) {
                                    closedAndClipped = path.getAttribute("mask") && path.getAttribute("d").includes(" Z");
                                    currentBorderWidth = parseFloat(path.getAttribute("stroke-width"));
                                    newBorderWidth = closedAndClipped ? overridedStyle.borderWidth * 2 : overridedStyle.borderWidth;
                                    path.setAttribute("stroke-width", newBorderWidth);
                                }
                            }
                            
                            borderStyle += "stroke-width:" + newBorderWidth + "; ";

                            if (!closedAndClipped) {
                                arrowHeadStyle += "stroke-width: " + (newBorderWidth - originalBorderWidth) + "; ";
                            }

                            if (overridedStyle.linePatternArray) {
                                var linePattern = overridedStyle.linePatternArray.map(x => x * overridedStyle.borderWidth);
                                borderStyle += "stroke-dasharray:" + linePattern.join(",") + "; ";
                            }
                        }

                    }

                    if (borderStyle.length > 0) {
                        borderStyle += transition;
                    }
                    if (arrowHeadStyle.length > 0) {
                        arrowHeadStyle += transition;
                    }

                    styleHtml += "#" + svgId + " .stroke { " + borderStyle + " } ";
                    styleHtml += "#" + svgId + " .arrowhead { " + arrowHeadStyle + " } ";

                    if($ax.public.fn.IsCheckBox(obj.type)) {
                        var checkWidth = 3 * obj.buttonSize / 14;
                        
                        styleHtml += "#" + svgId + " .stroke.btn_check { stroke-width: " + checkWidth + "; } ";
                    }
                }

                function setOuterShadow() {
                    if (overridedStyle.outerShadow && overridedStyle.outerShadow.on) {
                        var dropShadowStyle = "#" + svgId + " { filter: drop-shadow(" +
                            overridedStyle.outerShadow.offsetX + "px " +
                            overridedStyle.outerShadow.offsetY + "px " +
                            overridedStyle.outerShadow.blurRadius + "px " +
                            _getCssColor(overridedStyle.outerShadow.color) + "); " +
                            transition + "} ";
                        styleHtml += dropShadowStyle;
                    } else {
                        var dropShadowStyle = "#" + svgId + " { " + transition + " } ";
                        styleHtml += dropShadowStyle
                    }
                }

                function setInnerShadow() {
                    if (overridedStyle.innerShadow && overridedStyle.innerShadow.on) {
                        var filterTag = svgTag.querySelector("filter");
                        var innerShadowFilterId = filterTag && filterTag.getAttribute("id");
                        if (innerShadowFilterId) {
                            var feOffset = filterTag.querySelector("#offset");
                            feOffset.setAttribute("dx", overridedStyle.innerShadow.offsetX);
                            feOffset.setAttribute("dy", overridedStyle.innerShadow.offsetY);

                            var feBlur = filterTag.querySelector("#blur");
                            feBlur.setAttribute("stdDeviation", overridedStyle.innerShadow.blurRadius / 2);

                            var feMorphology = filterTag.querySelector("#morphology");
                            feMorphology.setAttribute("radius", +overridedStyle.borderWidth + overridedStyle.innerShadow.spread);

                            var feFlood = filterTag.querySelector("#color");
                            feFlood.setAttribute("flood-color", _getCssColor(overridedStyle.innerShadow.color));
                        } else {
                            var defsTag = svgTag.getElementsByTagName("defs")[0];
                            if (!defsTag) {
                                defsTag = document.createElementNS(svgns, "defs");
                                svgTag.appendChild(defsTag);
                            }
                            filterTag = document.createElementNS(svgns, "filter");
                            innerShadowFilterId = svgId + "innerShadowFilter";
                            filterTag.setAttribute("x", "-50%");
                            filterTag.setAttribute("y", "-50%");
                            filterTag.setAttribute("width", "200%");
                            filterTag.setAttribute("height", "200%");
                            filterTag.setAttribute("filterUnits", "objectBoundingBox");
                            filterTag.setAttribute("id", innerShadowFilterId);

                            var feOffset = document.createElementNS(svgns, "feOffset");
                            feOffset.setAttribute("dx", overridedStyle.innerShadow.offsetX);
                            feOffset.setAttribute("dy", overridedStyle.innerShadow.offsetY);
                            feOffset.setAttribute("in", "SourceGraphic");
                            feOffset.setAttribute("result", "offset");
                            feOffset.setAttribute("id", "offset");
                            filterTag.appendChild(feOffset);

                            var feMorphology = document.createElementNS(svgns, "feMorphology");
                            feMorphology.setAttribute("radius", +overridedStyle.borderWidth + overridedStyle.innerShadow.spread);
                            feMorphology.setAttribute("operator", "erode");
                            feMorphology.setAttribute("in", "offset");
                            feMorphology.setAttribute("result", "morphology");
                            feMorphology.setAttribute("id", "morphology");
                            filterTag.appendChild(feMorphology);

                            var feBlur = document.createElementNS(svgns, "feGaussianBlur");
                            feBlur.setAttribute("stdDeviation", overridedStyle.innerShadow.blurRadius / 2);
                            feBlur.setAttribute("in", "morphology");
                            feBlur.setAttribute("result", "blur");
                            feBlur.setAttribute("id", "blur");
                            filterTag.appendChild(feBlur);

                            var feComposite1 = document.createElementNS(svgns, "feComposite");
                            feComposite1.setAttribute("in2", "blur");
                            feComposite1.setAttribute("operator", "out");
                            feComposite1.setAttribute("in", "SourceGraphic");
                            feComposite1.setAttribute("result", "inverse");
                            feComposite1.setAttribute("id", "inverse");
                            filterTag.appendChild(feComposite1);

                            var feFlood = document.createElementNS(svgns, "feFlood");
                            feFlood.setAttribute("flood-color", _getCssColor(overridedStyle.innerShadow.color));
                            feFlood.setAttribute("in", "inverse");
                            feFlood.setAttribute("result", "color");
                            feFlood.setAttribute("id", "color");
                            filterTag.appendChild(feFlood);

                            var feComposite2 = document.createElementNS(svgns, "feComposite");
                            feComposite2.setAttribute("in2", "inverse");
                            feComposite2.setAttribute("operator", "in");
                            feComposite2.setAttribute("in", "color");
                            feComposite2.setAttribute("result", "shadow");
                            feComposite2.setAttribute("id", "shadow");
                            filterTag.appendChild(feComposite2);

                            var feComposite3 = document.createElementNS(svgns, "feComposite");
                            feComposite3.setAttribute("in2", "SourceGraphic");
                            feComposite3.setAttribute("operator", "over");
                            feComposite3.setAttribute("in", "shadow");
                            filterTag.appendChild(feComposite3);

                            defsTag.appendChild(filterTag);
                        }
                        var innerShadowCss = "#" + svgId + " .fill, " + "#" + svgId + " .background-image { filter:url('#" + innerShadowFilterId + "');} ";
                        styleHtml += innerShadowCss;
                    }
                }

                //if the current gradient has the same number of stops
                //add the css for each stop to animate it
                //otherwise create a new gradient and apply it
                function insertLinearGradient(gradientId, styleFill, propName) {
                    var defsTag = svgTag.getElementsByTagName("defs")[0];
                    if (!defsTag) {
                        defsTag = document.createElementNS(svgns, "defs");
                        svgTag.appendChild(defsTag);
                    }
                    var stops = styleFill.stops;

                    //clean up old non-default gradients
                    defsTag.querySelectorAll(".temp_" + propName).forEach(e => e.remove());

                    var currentFill = propName == "fill" ? window.getComputedStyle(svgTag.querySelector(".fill")).fill : window.getComputedStyle(svgTag.querySelector(".stroke")).stroke;
                    if (currentFill.indexOf("url") > -1) {
                        var currentGradId = currentFill.substring(currentFill.indexOf('#') + 1, currentFill.indexOf('")'));
                        var currentGrad = document.getElementById(currentGradId);
                        if (currentGrad && currentGrad.tagName === "linearGradient") {
                            var currentStops = currentGrad.querySelectorAll("stop");
                            if (currentStops.length == stops.length) {
                                for (var i = 0, length = stops.length; i < length; i++) {
                                    styleHtml += "#" + currentGradId + " stop:nth-of-type(" + (i + 1) + ") { stop-color:" + _getColorFromArgbColorProp(stops[i].color) + " } ";
                                }
                                styleHtml += "#" + currentGradId + " stop { " + transition + " } ";

                                currentGrad.setAttribute("x1", viewBoxX + styleFill.startPoint.x * overridedStyle.size.width);
                                currentGrad.setAttribute("y1", viewBoxY + styleFill.startPoint.y * overridedStyle.size.height);
                                currentGrad.setAttribute("x2", viewBoxX + styleFill.endPoint.x * overridedStyle.size.width);
                                currentGrad.setAttribute("y2", viewBoxY + styleFill.endPoint.y * overridedStyle.size.height);

                                return false;
                            }
                        }
                    }

                    var gradient = document.createElementNS(svgns, "linearGradient");
                    for (var i = 0, length = stops.length; i < length; i++) {
                        var stop = document.createElementNS(svgns, "stop");
                        stop.setAttribute("offset", stops[i].offset);
                        stop.setAttribute("stop-color", _getColorFromArgbColorProp(stops[i].color));
                        gradient.appendChild(stop);
                    }

                    gradient.id = gradientId;
                    gradient.classList.add("temp_" + propName);
                    gradient.setAttribute("gradientUnits", "userSpaceOnUse");
                    gradient.setAttribute("x1", viewBoxX + styleFill.startPoint.x * overridedStyle.size.width);
                    gradient.setAttribute("y1", viewBoxY + styleFill.startPoint.y * overridedStyle.size.height);
                    gradient.setAttribute("x2", viewBoxX + styleFill.endPoint.x * overridedStyle.size.width);
                    gradient.setAttribute("y2", viewBoxY + styleFill.endPoint.y * overridedStyle.size.height);
                    defsTag.appendChild(gradient);

                    return true;
                }

                function insertRadialGradient(gradientId, styleFill, propName) {
                    var defsTag = svgTag.getElementsByTagName("defs")[0];
                    if (!defsTag) {
                        defsTag = document.createElementNS(svgns, "defs");
                        svgTag.appendChild(defsTag);
                    }
                    var stops = styleFill.stops;

                    var center = { x: overridedStyle.size.width * styleFill.center.x, y: overridedStyle.size.height * styleFill.center.y };
                    var axis1 = { x: overridedStyle.size.width * styleFill.axis1.x, y: overridedStyle.size.height * styleFill.axis1.y };
                    var axis2 = { x: overridedStyle.size.width * styleFill.axis2.x, y: overridedStyle.size.height * styleFill.axis2.y };
                    var length1 = Math.sqrt((axis1.x - center.x) * (axis1.x - center.x) + (axis1.y - center.y) * (axis1.y - center.y));
                    var length2 = Math.sqrt((axis2.x - center.x) * (axis2.x - center.x) + (axis2.y - center.y) * (axis2.y - center.y));
                    var radius, xScale, yScale;
                    if (length1 < length2) {
                        xScale = length1 / length2;
                        yScale = 1;
                        radius = length2;
                    } else {
                        yScale = length2 / length1;
                        xScale = 1;
                        radius = length1;
                    }
                    var tx = viewBoxX + center.x;
                    var ty = viewBoxY + center.y;
                    var scaleMat = $ax.public.fn.matrixMultiplyMatrix({ m11: xScale, m12: 0, m21: 0, m22: yScale, tx: tx, ty: ty }, { m11: 1, m12: 0, m21: 0, m22: 1, tx: -tx, ty: -ty });

                    var sinAngle = (axis1.y - center.y) / length1;
                    var cosAngle = (axis1.x - center.x) / length1;
                    var rotateAroundMat = $ax.public.fn.matrixMultiplyMatrix({ m11: cosAngle, m22: cosAngle, m12: -sinAngle, m21: sinAngle, tx: tx, ty: ty }, { m11: 1, m12: 0, m21: 0, m22: 1, tx: -tx, ty: -ty });
                    var transformMat = $ax.public.fn.matrixMultiplyMatrix(rotateAroundMat, scaleMat);
                    var transformMatAttr = "matrix(" + transformMat.m11 + " " + transformMat.m21 + " " + transformMat.m12 + " " + transformMat.m22 + " " + transformMat.tx + " " + transformMat.ty + ")";

                    //clean up old non-default gradients
                    defsTag.querySelectorAll(".temp_" + propName).forEach(e => e.remove());

                    var currentFill = propName == "fill" ? window.getComputedStyle(svgTag.querySelector(".fill")).fill : window.getComputedStyle(svgTag.querySelector(".stroke")).stroke;
                    if (currentFill.indexOf("url") > -1) {
                        var currentGradId = currentFill.substring(currentFill.indexOf('#') + 1, currentFill.indexOf('")'));
                        var currentGrad = document.getElementById(currentGradId);
                        if (currentGrad && currentGrad.tagName === "radialGradient") {
                            var currentStops = currentGrad.querySelectorAll("stop");
                            if (currentStops.length == stops.length) {
                                for (var i = 0, length = stops.length; i < length; i++) {
                                    styleHtml += "#" + currentGradId + " stop:nth-of-type(" + (i + 1) + ") { stop-color:" + _getColorFromArgbColorProp(stops[i].color) + " } ";
                                }
                                styleHtml += "#" + currentGradId + " stop { " + transition + " } ";
                                currentGrad.setAttribute("cx", viewBoxX + center.x);
                                currentGrad.setAttribute("cy", viewBoxY + center.y);
                                currentGrad.setAttribute("r", radius);
                                currentGrad.setAttribute("gradientTransform", transformMatAttr);
                                currentGrad.setAttribute("gradientUnits", "userSpaceOnUse");

                                return false;
                            }
                        }
                    }

                    var gradient = document.createElementNS(svgns, "radialGradient");
                    for (var i = 0, length = stops.length; i < length; i++) {
                        var stop = document.createElementNS(svgns, "stop");
                        stop.setAttribute("offset", stops[i].offset);
                        stop.setAttribute("stop-color", _getColorFromArgbColorProp(stops[i].color));
                        gradient.appendChild(stop);
                    }

                    gradient.id = gradientId;
                    gradient.classList.add("temp_" + propName);
                    gradient.setAttribute("cx", viewBoxX + center.x);
                    gradient.setAttribute("cy", viewBoxY + center.y);
                    gradient.setAttribute("r", radius);
                    gradient.setAttribute("gradientTransform", transformMatAttr);
                    gradient.setAttribute("gradientUnits", "userSpaceOnUse");
                    defsTag.appendChild(gradient);

                    return true;
                }
            }

            var obj = $obj(id);
            if(obj.generateCompound) {
                for(var i = 0; i < obj.compoundChildren.length; i++) {
                    var componentId = obj.compoundChildren[i];
                    var childId = $ax.public.fn.getComponentId(id, componentId) + "_img";
                    //const container = document.getElementById(childId);
                //    const contentDoc = container.contentDocument;
                //    if(!contentDoc || contentDoc.URL == "about:blank") container.onload = () => applyStyleOverrides(container.contentDocument, overridedStyle);
                //    else applyStyleOverrides(contentDoc, overridedStyle);
                    applyStyleOverrides(childId, overridedStyle);
                }
            } else {
                const svgContainerId = id + "_img";
                const container = document.getElementById(svgContainerId);
                if (!container || container.tagName.toLowerCase() != "svg") return;
                //if($ax.public.fn.IsRadioButton(obj.type) || $ax.public.fn.IsCheckBox(obj.type)) {
                //    let imageKey = event + "~";
                //    const viewStr = parent.document.querySelector(".currentAdaptiveView").getAttribute("val");
                //    if(viewStr && viewStr !== "default") imageKey += viewStr;
                //    const data = obj.images[imageKey];
                //    if (data && container.data && !container.data.includes(data)) container.data = data;
                //}
                //const contentDoc = container.contentDocument;
                //if(!contentDoc || contentDoc.URL == "about:blank") container.onload = () => applyStyleOverrides(container.contentDocument, overridedStyle);
                //else applyStyleOverrides(contentDoc, overridedStyle);
                applyStyleOverrides(svgContainerId, overridedStyle);
            }
        }
        //else {

        //    function resetOverrides(svgContainerId) {
        //        var svgFills = document.getElementById(svgContainerId).contentDocument.querySelectorAll(".fill");
        //        svgFills.forEach((svgFill) => {
        //            svgFill.setAttribute("style", "");
        //        });
        //        var svgBorders = document.getElementById(svgContainerId).contentDocument.querySelectorAll(".stroke");
        //        svgBorders.forEach((svgBorder) => {
        //            svgBorder.setAttribute("style", "");
        //            svgBorder.setAttribute("stroke-dasharray", "");
        //        });
        //    }

        //    var object = $obj(id);
        //    if(object.generateCompound) {
        //        for(var i = 0; i < object.compoundChildren.length; i++) {
        //            var componentId = object.compoundChildren[i];
        //            var childId = $ax.public.fn.getComponentId(id, componentId) + "_img";
        //            resetOverrides(childId, overridedStyle);
        //        }
        //    } else {
        //        const svgContainerId = id + "_img";
        //        resetOverrides(svgContainerId, overridedStyle);
        //    }
        //}
    }

    var _applyImageAndTextJson = function (id, event) {
        _enableStateTransitions();

        const textId = $ax.GetTextPanelId(id);
        if(textId) _resetTextJson(id, textId);

        if($ax.public.fn.IsImageBox($obj(id).type)) {
            const imageUrl = $ax.adaptive.getImageForStateAndView(id, event);
            if(imageUrl) {
                _applyImage(id, imageUrl, event);
            }
        } else _applySvg(id, event);

        if (textId) {
            const overridedStyle = _computeAllOverrides(id, undefined, event, $ax.adaptive.currentViewId);
            var borderElement = document.getElementById(id + '_div');
            if($(borderElement).hasClass("bgImg")) borderElement = undefined;
            var textElement = document.getElementById(textId);
            if (!$.isEmptyObject(overridedStyle)) {
                var diagramObject = $ax.getObjectFromElementId(id);
                var fullStyle = _computeFullStyle(id, event, $ax.adaptive.currentViewId, overridedStyle);
                var padding = { top: 0, right: 0, bottom: 0, left: 0 };
                if (fullStyle.paddingTop) padding.top = +fullStyle.paddingTop;
                if (fullStyle.paddingBottom) padding.bottom = +fullStyle.paddingBottom;
                if (fullStyle.paddingLeft) padding.left = +fullStyle.paddingLeft;
                if (fullStyle.paddingRight) padding.right = +fullStyle.paddingRight;
                var newSize = _applyTextStyle(textId, overridedStyle, padding);
                if (borderElement && textElement && (diagramObject.autoFitHeight || diagramObject.autoFitWidth)) {
                    if (diagramObject.autoFitHeight) {
                        var height = newSize.height;
                        borderElement.style.height = height + 'px';
                        textElement.style.top = 0;
                    }
                    if (diagramObject.autoFitWidth) {
                        var width = newSize.width;
                        borderElement.style.width = width + 'px';
                        textElement.style.left = 0;
                    }
                }
            } else if (borderElement && textElement) {
                var parentElement = document.getElementById(id);
                if (parentElement) {
                    borderElement.style.height = parentElement.style.height;
                    borderElement.style.width = parentElement.style.width;
                }
                textElement.style.top = '';
                textElement.style.left = '';
            }
        }

        _updateStateClasses(
            [
                id,
                $ax.repeater.applySuffixToElementId(id, '_div'),
                $ax.repeater.applySuffixToElementId(id, '_input')
            ], event, false
        );
    };
    
    let _updateStateClasses = function(ids, event, addMouseOverOnMouseDown) {
        for(let i = 0; i < ids.length; i++) {
            _updateStateClassesHelper(ids[i], event, addMouseOverOnMouseDown);
        }
    };

    let _updateStateClassesHelper = function(id, event, addMouseOverOnMouseDown) {
        const jobj = $jobj(id);

        const isMouseDownEvent = _stateHasMouseDown(event);

        for (let i = 0; i < ALL_STATES_WITH_CSS_CLASS.length; i++) jobj.removeClass(ALL_STATES_WITH_CSS_CLASS[i]);

        if (addMouseOverOnMouseDown && isMouseDownEvent || _stateHasMouseOver(event)) jobj.addClass(MOUSE_OVER);
        if (isMouseDownEvent) jobj.addClass(MOUSE_DOWN);
        if (_stateHasFocused(event)) jobj.addClass(FOCUSED);
        if (_stateHasSelected(event)) jobj.addClass(SELECTED);
        if (_stateHasError(event)) jobj.addClass(ERROR);
        if (_stateHasDisabled(event)) jobj.addClass(DISABLED);
        if (_stateHasHint(event)) jobj.addClass(HINT);
    };

    /* -------------------

    here's the algorithm in a nutshell:
    [DOWN] -- refers to navigation down the view inheritance heirarchy (default to most specific)
    [UP] -- navigate up the heirarchy

    ComputeAllOverrides (object):
    All view styles [DOWN]
    If hyperlink
    - DO ComputeStateStyle for parent object
    - if (MouseOver || MouseDown) 
    - linkMouseOver Style
    - if (MouseDown) 
    - linkMouseDown style
    - ComputeStateStyleForViewChain (parent, STATE)
    
    if (MouseDown) DO ComputeStateStyleForViewChain for object, mouseOver
    DO ComputeStateStyleForViewChain for object, style


    ComputeStateStyleForViewChain (object, STATE)
    FIRST STATE state style [UP] the chain OR default object STATE style

    ------------------- */

    var FONT_PROPS = {
        'typeface': true,
        'fontName': true,
        'fontWeight': true,
        'fontStyle': true,
        'fontStretch': true,
        'fontSize': true,
        'underline': true,
        'foreGroundFill': true,
        'horizontalAlignment': true,
        'letterCase': true,
        'strikethrough': true
    };

    var _getViewIdChain = $ax.style.getViewIdChain = function(currentViewId, id, diagramObject) {
        var viewIdChain;
        if (diagramObject.owner.type == 'Axure:Master' || diagramObject.owner.type == 'referenceDiagramObject') {
            //set viewIdChain to the chain from the parent RDO
            var parentRdoId;
            if (diagramObject.owner.type == 'referenceDiagramObject') parentRdoId = diagramObject.owner.scriptIds[0];
            if (!parentRdoId) parentRdoId = $ax('#' + id).getParents(true, ['rdo'])[0][0];
            var rdoState = $ax.style.generateState(parentRdoId);
            var rdoStyle = $ax.style.computeFullStyle(parentRdoId, rdoState, currentViewId);
            var viewOverride = rdoStyle.viewOverride;
            viewIdChain = $ax.adaptive.getMasterAdaptiveIdChain(diagramObject.owner.type == 'referenceDiagramObject' ? diagramObject.owner.masterId : diagramObject.owner.packageId, viewOverride);
        } else {
            viewIdChain = $ax.adaptive.getAdaptiveIdChain(currentViewId);
        }
        return viewIdChain;
    }

    var _computeAllOverrides = $ax.style.computeAllOverrides = function(id, parentId, state, currentViewId) {
        var computedStyle = {};
        if(parentId) computedStyle = _computeAllOverrides(parentId, null, state, currentViewId);

        var diagramObject = $ax.getObjectFromElementId(id);

        var viewIdChain = _getViewIdChain(currentViewId, id, diagramObject);
        var excludeFont = _shapesWithSetRichText[id];
        for(var i = 0; i < viewIdChain.length; i++) {
            var viewId = viewIdChain[i];
            var style = diagramObject.adaptiveStyles[viewId];
            if(style) {
                // we want to exclude the normal font style for shapes where the rich text has been set with an interaction
                // so we copy the style so we don't modify the original, then delete all the font props.
                if(excludeFont) {
                    style = $ax.deepCopy(style);
                    for(var prop in FONT_PROPS) delete style[prop];
                }

                if(style) {
                    var customStyle = style.baseStyle && $ax.document.stylesheet.stylesById[style.baseStyle];
                    //make sure not to extend the customStyle this can mutate it for future use
                    $.extend(computedStyle, customStyle);
                }
                $.extend(computedStyle, style);
            }
        }

        // order matters so higher priority styles override lower priority
        $.extend(computedStyle, _computeStateStyleForViewChain(diagramObject, NORMAL, viewIdChain, true));
        if (_stateHasMouseOver(state)) {
            $.extend(computedStyle, _computeStateStyleForViewChain(diagramObject, MOUSE_OVER, viewIdChain, true));
        }
        if (_stateHasMouseDown(state)) {
            $.extend(computedStyle, _computeStateStyleForViewChain(diagramObject, MOUSE_DOWN, viewIdChain, true));
        }
        if (_stateHasFocused(state)) {
            $.extend(computedStyle, _computeStateStyleForViewChain(diagramObject, FOCUSED, viewIdChain, true));
        }
        if (_stateHasSelected(state)) {
            $.extend(computedStyle, _computeStateStyleForViewChain(diagramObject, SELECTED, viewIdChain, true));
        }
        if (_stateHasError(state)) {
            $.extend(computedStyle, _computeStateStyleForViewChain(diagramObject, ERROR, viewIdChain, true));
        }
        if (_stateHasHint(state)) {
            $.extend(computedStyle, _computeStateStyleForViewChain(diagramObject, HINT, viewIdChain, true));
        }
        if (_stateHasDisabled(state)) {
            $.extend(computedStyle, _computeStateStyleForViewChain(diagramObject, DISABLED, viewIdChain, true));
        }

        return _removeUnsupportedProperties(computedStyle, diagramObject);
    };

    var _computeStateStyleForViewChain = function(diagramObject, state, viewIdChain, excludeNormal) {
        var styleObject = diagramObject;
        while(styleObject.isContained) styleObject = styleObject.parent;

        var adaptiveStyles = styleObject.adaptiveStyles;

        for(var i = viewIdChain.length - 1; i >= 0; i--) {
            var viewId = viewIdChain[i];
            var viewStyle = adaptiveStyles[viewId];
            var stateStyle = viewStyle && _getFullStateStyle(viewStyle, state, excludeNormal);
            if (stateStyle) return $.extend({}, stateStyle);
            else if (viewStyle && viewStyle.stateStyles) return {}; //stateStyles are overriden but states could be null
        }

        // we dont want to actually include the object style because those are not overrides, hence the true for "excludeNormal" and not passing the val through
        var stateStyleFromDefault = _getFullStateStyle(styleObject.style, state, true);
        return $.extend({}, stateStyleFromDefault);
    };

    // returns the full effective style for an object in a state state and view
    var _computeFullStyle = $ax.style.computeFullStyle = function(id, state, currentViewId, overrides) {
        var obj = $obj(id);
        if (!overrides) overrides = _computeAllOverrides(id, undefined, state, currentViewId);
        // get style for current state
        var dynamicPanelStyle = _getCurrentPanelDiagramStyle(id);

        // todo: account for image box
        var objStyle = obj.style;
        var customStyle = objStyle.baseStyle && $ax.document.stylesheet.stylesById[objStyle.baseStyle];
        var returnVal = $.extend({}, $ax.document.stylesheet.defaultStyle, customStyle, objStyle, dynamicPanelStyle, overrides);
        return _removeUnsupportedProperties(returnVal, obj);
    };

    var _getCurrentPanelDiagramStyle = function (id) {
        var diagramObj = $ax.visibility.GetCurrentPanelDiagram(id);
        if (diagramObj) {
            return diagramObj.style;
        }
        return {};
    };

    var _removeUnsupportedProperties = function(style, object) {
        // for now all we need to do is remove padding from checkboxes and radio buttons
        //if ($ax.public.fn.IsRadioButton(object.type) || $ax.public.fn.IsCheckBox(object.type)) {
        //    style.paddingTop = 0;
        //    style.paddingLeft = 0;
        //    style.paddingRight = 0;
        //    style.paddingBottom = 0;
        //}
        //if ($ax.public.fn.IsTextBox(object.type) || $ax.public.fn.IsTextArea(object.type) || $ax.public.fn.IsButton(object.type)
        //    || $ax.public.fn.IsListBox(object.type) || $ax.public.fn.IsComboBox(object.type)) {
        //    if (object.images && style.fill) delete style['fill'];
        //}

        return style;
    };

    var _getFullStateStyle = function(style, state, excludeNormal) {
        //'normal' is needed because now DiagramObjects get their image from the Style and unapplying a rollover needs the image
        var stateStyle = state == NORMAL && !excludeNormal ? style : style && style.stateStyles && style.stateStyles[state];
        if(stateStyle) {
            var customStyle = stateStyle.baseStyle && $ax.document.stylesheet.stylesById[stateStyle.baseStyle];
            //make sure not to extend the customStyle this can mutate it for future use
            return $.extend({}, customStyle, stateStyle);
        }
        return undefined;
    };

    // commented this out for now... we actually will probably need it for ie
    var _applyOpacityFromStyle = $ax.style.applyOpacityFromStyle = function(id, style) {
        return;
        var opacity = style.opacity || '';
        $jobj(id).children().css('opacity', opacity);
    };

    var _initialize = function() {
        //$ax.style.initializeObjectTextAlignment($ax('*'));
    };
    $ax.style.initialize = _initialize;

    //var _initTextAlignment = function(elementId) {
    //    var textId = $ax.GetTextPanelId(elementId);
    //    if(textId) {
    //        _storeIdToAlignProps(textId);
    //        // now handle vertical alignment
    //        if(_getObjVisible(textId)) {
    //            //_setTextAlignment(textId, _idToAlignProps[textId], false);
    //            _setTextAlignment(textId);
    //        }
    //    }
    //};

    //$ax.style.initializeObjectTextAlignment = function(query) {
    //    query.filter(function(diagramObject) {
    //        return $ax.public.fn.IsVector(diagramObject.type) || $ax.public.fn.IsImageBox(diagramObject.type);
    //    }).each(function(diagramObject, elementId) {
    //        if($jobj(elementId).length == 0) return;
    //        _initTextAlignment(elementId);
    //    });
    //};

    //$ax.style.initializeObjectTextAlignment = function (query) {
    //    var textIds = [];
    //    query.filter(function(diagramObject) {
    //        return $ax.public.fn.IsVector(diagramObject.type) || $ax.public.fn.IsImageBox(diagramObject.type);
    //    }).each(function(diagramObject, elementId) {
    //        if($jobj(elementId).length == 0) return;
    //        var textId = $ax.GetTextPanelId(elementId);
    //        if(textId) {
    //            _storeIdToAlignProps(textId);
    //            textIds.push(textId);
    //        }
    //    });

    //    $ax.style.setTextAlignment(textIds);
    //};

    //var _getPadding = $ax.style.getPadding = function (textId) {
    //    var shapeId = $ax.GetShapeIdFromText(textId);
    //    var shapeObj = $obj(shapeId);
    //    var state = _generateState(shapeId);

    //    var style = _computeFullStyle(shapeId, state, $ax.adaptive.currentViewId);
    //    var vAlign = style.verticalAlignment || 'middle';

    //    var paddingLeft = Number(style.paddingLeft) || 0;
    //    paddingLeft += (Number(shapeObj && shapeObj.extraLeft) || 0);
    //    var paddingTop = style.paddingTop || 0;
    //    var paddingRight = style.paddingRight || 0;
    //    var paddingBottom = style.paddingBottom || 0;
    //    return { vAlign: vAlign, paddingLeft: paddingLeft, paddingTop: paddingTop, paddingRight: paddingRight, paddingBottom: paddingBottom };
    //}

    //var _storeIdToAlignProps = function(textId) {
    //    _idToAlignProps[textId] = _getPadding(textId);
    //};

    var _applyImage = $ax.style.applyImage = function (id, imgUrl, state) {
            var object = $obj(id);
            if (object.generateCompound) {
                for (var i = 0; i < object.compoundChildren.length; i++) {
                    var componentId = object.compoundChildren[i];
                    var childId = $ax.public.fn.getComponentId(id, componentId);
                    var childImgQuery = $jobj(childId + '_img');
                    //childImgQuery.attr('src', imgUrl[componentId]);
                    childImgQuery.attr('data', imgUrl[componentId]);

                    _updateStateClasses(
                        [
                            childId + '_img',
                            childId
                        ], state, false
                    );
                }
            } else {
                var imgQuery = $jobj($ax.GetImageIdFromShape(id));
                //it is hard to tell if setting the image or the class first causing less flashing when adding shadows.
                imgQuery.attr('src', imgUrl);

                var svgQuery = $jobj($ax.GetSvgIdFromShape(id));
                svgQuery.attr('data', imgUrl);

                _updateStateClasses(
                    [
                        id,
                        $ax.GetImageIdFromShape(id)
                    ], state, false
                );
                if (imgQuery.parents('a.basiclink').length > 0) imgQuery.css('border', 'none');
            }

    };

    $ax.public.fn.getComponentId = function (id, componentId) {
        var idParts = id.split('-');
        idParts[0] = idParts[0] + componentId;
        return idParts.join('-');
    }

    var _resetTextJson = function(id, textid) {
        // reset the opacity
        $jobj(id).children().css('opacity', '');

        var cacheObject = _originalTextCache[textid];
        if(cacheObject) {
            _transformTextWithVerticalAlignment(textid, function() {
                var styleCache = cacheObject.styleCache;
                var textQuery = $('#' + textid);
                textQuery.find('*').each(function(index, element) {
                    element.style.cssText = styleCache[element.id];
                });
            });
        }
    };

    // Preserves the alingment for the element textid after executing transformFn

    //var _getRtfElementHeight = function(rtfElement) {
    //    if(rtfElement.innerHTML == '') rtfElement.innerHTML = '&nbsp;';

    //    // To handle render text as image
    //    //var images = $(rtfElement).children('img');
    //    //if(images.length) return images.height();
    //    return rtfElement.offsetHeight;
    //};

    // why microsoft decided to default to round to even is beyond me...
    //var _roundToEven = function(number) {
    //    var numString = number.toString();
    //    var parts = numString.split('.');
    //    if(parts.length == 1) return number;
    //    if(parts[1].length == 1 && parts[1] == '5') {
    //        var wholePart = Number(parts[0]);
    //        return wholePart % 2 == 0 ? wholePart : wholePart + 1;
    //    } else return Math.round(number);
    //};

    //var _suspendTextAlignment = 0;
    //var _suspendedTextIds = [];
    //$ax.style.startSuspendTextAlignment = function() {
    //    _suspendTextAlignment++;
    //}
    //$ax.style.resumeSuspendTextAlignment = function () {
    //    _suspendTextAlignment--;
    //    if(_suspendTextAlignment == 0) $ax.style.setTextAlignment(_suspendedTextIds);
    //}

    var _transformTextWithVerticalAlignment = $ax.style.transformTextWithVerticalAlignment = function(textId, transformFn) {
        if(!_originalTextCache[textId]) {
            $ax.style.CacheOriginalText(textId);
        }

        var rtfElement = window.document.getElementById(textId);
        if(!rtfElement) return;

        return transformFn();

        //_storeIdToAlignProps(textId);

        //if (_suspendTextAlignment) {
        //    _suspendedTextIds.push(textId);
        //    return;
        //}

        //$ax.style.setTextAlignment([textId]);
    };

    // this is for vertical alignments set on hidden objects
    //var _idToAlignProps = {};
    
    //$ax.style.updateTextAlignmentForVisibility = function (textId) {
    //    var textObj = $jobj(textId);
    //    // must check if parent id exists. Doesn't exist for text objs in check boxes, and potentially elsewhere.
    //    var parentId = textObj.parent().attr('id');
    //    if (parentId && $ax.visibility.isContainer(parentId)) return;

    //    //var alignProps = _idToAlignProps[textId];
    //    //if(!alignProps || !_getObjVisible(textId)) return;
    //    //if (!alignProps) return;

    //    //_setTextAlignment(textId, alignProps);
    //    _setTextAlignment(textId);
    //};

    var _getObjVisible = _style.getObjVisible = function (id) {
        var element = document.getElementById(id);
        return element && (element.offsetWidth || element.offsetHeight);
    };

    //$ax.style.setTextAlignment = function (textIds) {
        
    //    var getTextAlignDim = function(textId, alignProps) {
    //        var dim = {};
    //        var vAlign = alignProps.vAlign;
    //        var paddingTop = Number(alignProps.paddingTop);
    //        var paddingBottom = Number(alignProps.paddingBottom);
    //        var paddingLeft = Number(alignProps.paddingLeft);
    //        var paddingRight = Number(alignProps.paddingRight);

    //        var topParam = 0.0;
    //        var bottomParam = 1.0;
    //        var leftParam = 0.0;
    //        var rightParam = 1.0;

    //    var textObj = $jobj(textId);
    //    var textObjParent = textObj.offsetParent();
    //    var parentId = textObjParent.attr('id');
    //    if(!parentId) {
    //        // Only case should be for radio/checkbox that get the label now because it must be absolute positioned for animate (offset parent ignored it before)
    //        textObjParent = textObjParent.parent();
    //        parentId = textObjParent.attr('id');
    //    }

    //    parentId = $ax.visibility.getWidgetFromContainer(textObjParent.attr('id'));
    //    textObjParent = $jobj(parentId);
    //    var parentObj = $obj(parentId);
    //    if(parentObj['bottomTextPadding']) bottomParam = parentObj['bottomTextPadding'];
    //    if(parentObj['topTextPadding']) topParam = parentObj['topTextPadding'];
    //    if(parentObj['leftTextPadding']) leftParam = parentObj['leftTextPadding'];
    //    if(parentObj['rightTextPadding']) rightParam = parentObj['rightTextPadding'];

    //    // smart shapes are mutually exclusive from compound vectors.
    //    var isConnector = parentObj.type == $ax.constants.CONNECTOR_TYPE;
    //    if(isConnector) return;

    //        var axTextObjectParent = $ax('#' + textObjParent.attr('id'));


    //        var jDims = textObj.css(['width','left','top']);
    //        var oldWidth = $ax.getNumFromPx(jDims['width']);
    //        var oldLeft = $ax.getNumFromPx(jDims['left']);
    //        var oldTop = $ax.getNumFromPx(jDims['top']);

    //        var newTop = 0;
    //        var newLeft = 0.0;

    //        var size = axTextObjectParent.size();
    //        var width = size.width;
    //        var height = size.height;
    //        //var width = axTextObjectParent.width();
    //        //var height = axTextObjectParent.height();

    //        // If text rotated need to handle getting the correct width for text based on bounding rect of rotated parent.
    //        var boundingRotation = -$ax.move.getRotationDegreeFromElement(textObj[0]);
    //        var boundingParent = $axure.fn.getBoundingSizeForRotate(width, height, boundingRotation);
    //        var extraLeftPadding = (width - boundingParent.width) / 2;
    //        width = boundingParent.width;
    //        var relativeTop = 0.0;
    //        relativeTop = height * topParam;
    //        var containerHeight = height * bottomParam - relativeTop;

    //        newLeft = paddingLeft + extraLeftPadding + width * leftParam;

    //        var newWidth = width * (rightParam - leftParam) - paddingLeft - paddingRight;

    //        var horizChange = newWidth != oldWidth || newLeft != oldLeft;
    //        if(horizChange) {
    //            dim.left = newLeft;
    //            dim.width = newWidth;
    //            //textObj.css('left', newLeft);
    //            //textObj.width(newWidth);
    //        }

    //        var textHeight = _getRtfElementHeight(textObj[0]);

    //        if(vAlign == "middle")
    //            newTop = _roundToEven(relativeTop + (containerHeight - textHeight + paddingTop - paddingBottom) / 2);
    //        else if(vAlign == "bottom")
    //            newTop = _roundToEven(relativeTop + containerHeight - textHeight - paddingBottom);
    //        else newTop = _roundToEven(paddingTop + relativeTop);
    //        var vertChange = oldTop != newTop;
    //        if (vertChange) dim.top = newTop; //textObj.css('top', newTop + 'px');

    //        return dim;
    //    };

    //    var applyTextAlignment = function(textId, dim) {
    //        var textObj = $jobj(textId);
    //        if(dim.left) {
    //            textObj.css('left', dim.left);
    //            textObj.width(dim.width);
    //        }
    //        if(dim.top) textObj.css('top', dim.top);

    //        if((dim.top || dim.left)) _updateTransformOrigin(textId);
    //    };

    //    var idToDim = [];
    //    for (var i = 0; i < textIds.length; i++) {
    //        var textId = textIds[i];
    //        var alignProps = _idToAlignProps[textId];
    //        if (!alignProps || !_getObjVisible(textId)) continue;

    //        idToDim.push({ id: textId, dim: getTextAlignDim(textId, alignProps) });
    //    }

    //    for (var i = 0; i < idToDim.length; i++) {
    //        var info = idToDim[i];
    //        applyTextAlignment(info.id, info.dim);
    //    }
    //};

    //var _setTextAlignment = function(textId, alignProps, updateProps) {
    //    if(updateProps) _storeIdToAlignProps(textId);
    //    if(!alignProps) return;

    //    var vAlign = alignProps.vAlign;
    //    var paddingTop = Number(alignProps.paddingTop);
    //    var paddingBottom = Number(alignProps.paddingBottom);
    //    var paddingLeft = Number(alignProps.paddingLeft);
    //    var paddingRight = Number(alignProps.paddingRight);

    //    var topParam = 0.0;
    //    var bottomParam = 1.0;
    //    var leftParam = 0.0;
    //    var rightParam = 1.0;

    //    var textObj = $jobj(textId);
    //    var textObjParent = textObj.offsetParent();
    //    var parentId = textObjParent.attr('id');
    //    var isConnector = false;
    //    if(parentId) {
    //        parentId = $ax.visibility.getWidgetFromContainer(textObjParent.attr('id'));
    //        textObjParent = $jobj(parentId);
    //        var parentObj = $obj(parentId);
    //        if(parentObj['bottomTextPadding']) bottomParam = parentObj['bottomTextPadding'];
    //        if(parentObj['topTextPadding']) topParam = parentObj['topTextPadding'];
    //        if(parentObj['leftTextPadding']) leftParam = parentObj['leftTextPadding'];
    //        if(parentObj['rightTextPadding']) rightParam = parentObj['rightTextPadding'];

    //        // smart shapes are mutually exclusive from compound vectors.
    //        isConnector = parentObj.type == $ax.constants.CONNECTOR_TYPE;
    //    }
    //    if(isConnector) return;

    //    var axTextObjectParent = $ax('#' + textObjParent.attr('id'));

    //    var oldWidth = $ax.getNumFromPx(textObj.css('width'));
    //    var oldLeft = $ax.getNumFromPx(textObj.css('left'));
    //    var oldTop = $ax.getNumFromPx(textObj.css('top'));

    //    var newTop = 0;
    //    var newLeft = 0.0;

    //    var width = axTextObjectParent.width();
    //    var height = axTextObjectParent.height();

    //    // If text rotated need to handle getting the correct width for text based on bounding rect of rotated parent.
    //    var boundingRotation = -$ax.move.getRotationDegreeFromElement(textObj[0]);
    //    var boundingParent = $axure.fn.getBoundingSizeForRotate(width, height, boundingRotation);
    //    var extraLeftPadding = (width - boundingParent.width) / 2;
    //    width = boundingParent.width;
    //    var relativeTop = 0.0;
    //    relativeTop = height * topParam;
    //    var containerHeight = height * bottomParam - relativeTop;


    //    newLeft = paddingLeft + extraLeftPadding + width * leftParam;

    //    var newWidth = width * (rightParam - leftParam) - paddingLeft - paddingRight;

    //    var horizChange = newWidth != oldWidth || newLeft != oldLeft;
    //    if(horizChange) {
    //        textObj.css('left', newLeft);
    //        textObj.width(newWidth);
    //    }

    //    var textHeight = _getRtfElementHeight(textObj[0]);

    //    if(vAlign == "middle") newTop = _roundToEven(relativeTop + (containerHeight - textHeight + paddingTop - paddingBottom) / 2);
    //    else if(vAlign == "bottom") newTop = _roundToEven(relativeTop + containerHeight - textHeight - paddingBottom);
    //    else newTop = _roundToEven(paddingTop + relativeTop);
    //    var vertChange = oldTop != newTop;
    //    if(vertChange) textObj.css('top', newTop + 'px');

    //    if((vertChange || horizChange)) _updateTransformOrigin(textId);
    //};

    //var _updateTransformOrigin = function (textId) {
    //    var textObj = $jobj(textId);
    //    var parentId = textObj.parent().attr('id');
    //    if(!$obj(parentId).hasTransformOrigin) return;

    //    //var transformOrigin = textObj.css('-webkit-transform-origin') ||
    //    //        textObj.css('-moz-transform-origin') ||
    //    //            textObj.css('-ms-transform-origin') ||
    //    //                textObj.css('transform-origin');
    //    //if(transformOrigin) {
    //        var textObjParent = $ax('#' + textObj.parent().attr('id'));
    //        var newX = (textObjParent.width() / 2 - $ax.getNumFromPx(textObj.css('left')));
    //        var newY = (textObjParent.height() / 2 - $ax.getNumFromPx(textObj.css('top')));
    //        var newOrigin = newX + 'px ' + newY + 'px';
    //        textObj.css('-webkit-transform-origin', newOrigin);
    //        textObj.css('-moz-transform-origin', newOrigin);
    //        textObj.css('-ms-transform-origin', newOrigin);
    //        textObj.css('transform-origin', newOrigin);
    //    //}
    //};

    $ax.style.reselectElements = function () {
        // TODO
        console.log('reselect elements -- need to make sure selected/error/disabled are all correct');
        for(let id in _selectedWidgets) {
            // Only looking for the selected widgets that don't have their class set
            if(!_selectedWidgets[id] || _hasAnySelectedClass(id)) continue;

            const state = _generateFullState(id, undefined, true);
            _applyImageAndTextJson(id, state);
        }

        for(let id in _disabledWidgets) {
            // Only looking for the disabled widgets that don't have their class yet
            if (!_disabledWidgets[id] || _hasAnyDisabledClass(id)) continue;

            const state = _generateFullState(id, undefined, undefined, true);
            _applyImageAndTextJson(id, state);
        }
    };

    $ax.style.clearStateForRepeater = function(repeaterId) {
        var children = $ax.getChildElementIdsForRepeater(repeaterId);
        for(var i = 0; i < children.length; i++) {
            var id = children[i];
            delete _hintWidgets[id];
            delete _errorWidgets[id];
            delete _selectedWidgets[id];
            delete _disabledWidgets[id];
        }
    }

    _style.updateStateClass = function (repeaterId) {
        var subElementIds = $ax.getChildElementIdsForRepeater(repeaterId);
        for (var i = 0; i < subElementIds.length; i++) {
            _applyImageAndTextJson(subElementIds[i], $ax.style.generateState(subElementIds[i]));
        }
    }

    $ax.style.clearAdaptiveStyles = function() {
        for(var shapeId in _adaptiveStyledWidgets) {
            var repeaterId = $ax.getParentRepeaterFromScriptId(shapeId);
            if(repeaterId) continue;
            var elementId = $ax.GetButtonShapeId(shapeId);
            if(elementId) _applyImageAndTextJson(elementId, $ax.style.generateState(elementId));
        }

        _adaptiveStyledWidgets = {};
    };

    $ax.style.setAdaptiveStyle = function(shapeId, style, state) {
        _adaptiveStyledWidgets[$ax.repeater.getScriptIdFromElementId(shapeId)] = style;

        var textId = $ax.GetTextPanelId(shapeId);
        if(textId) _applyTextStyle(textId, style);

        const svgContainerId = shapeId + "_img";
        const container = document.getElementById(svgContainerId);
        if(container && container.tagName.toLowerCase() == "svg") {
            _applySvg(shapeId, state ?? NORMAL);

            if(style.size) {
                if($obj(shapeId).friendlyType == "Rectangle") {
                    $(container).css({ 'width': style.size.width, 'height': style.size.height });
                } else {
                    var baseStyle = _computeFullStyle(shapeId, NORMAL, "");
                    var oldSize = baseStyle.size;
                    var oldWidth = oldSize.width;
                    var oldHeight = oldSize.height;

                    container.style.transformOrigin = 'top left';
                    var oldTransformMatrix = new WebKitCSSMatrix(window.getComputedStyle(container).transform);
                    var oldScaleX = oldTransformMatrix.m11;
                    var oldScaleY = oldTransformMatrix.m22;
                    var scaleX = (style.size.width / oldWidth) * oldScaleX;
                    var scaleY = (style.size.height / oldHeight) * oldScaleY;
                    if(oldScaleX !== scaleX || oldScaleY !== scaleY) {
                        container.style.transform = 'Scale(' + scaleX + ', ' + scaleY + ')';
                    }
                }
            }
        }
        $ax.placeholderManager.refreshPlaceholder(shapeId);

        // removing this for now
        //        if(style.location) {
        //            $jobj(shapeId).css('top', style.location.x + "px")
        //                .css('left', style.location.y + "px");
        //        }
    };

    //-------------------------------------------------------------------------
    // _applyTextStyle
    //
    // Applies a rollover style to a text element.
    //       id : the id of the text object to set.
    //       styleProperties : an object mapping style properties to values. eg:
    //                         { 'fontWeight' : 'bold',
    //                           'fontStyle' : 'italic' }
    //-------------------------------------------------------------------------
    var _applyTextStyle = function (id, style, padding = {top:0, right: 0, bottom: 0, left: 0}) {
        return _transformTextWithVerticalAlignment(id, function() {
            var styleProperties = _getCssStyleProperties(style);
            var container = $('#' + id);
            var newSize = { width: container[0].offsetWidth, height: container[0].offsetHeight };
            var hasLineHeight = !!container.css('line-height');
            container.find('*').each(function(index, element) {
                _applyCssProps(element, styleProperties);
                var width = element.offsetWidth + padding.left + padding.right;
                var height = element.offsetHeight + padding.top + padding.bottom;
                if(width > newSize.width) newSize.width = width;
                if(!hasLineHeight && height > newSize.height) newSize.height = height;
            });
            return newSize;
        });
    };

    var _applyCssProps = function(element, styleProperties, applyAllStyle) {
        if(applyAllStyle) {
            var allProps = styleProperties.allProps;
            for(var prop in allProps) element.style[prop] = allProps[prop];
        } else {
            var nodeName = element.nodeName.toLowerCase();
            if(nodeName == 'p') {
                var parProps = styleProperties.parProps;
                for(prop in parProps) element.style[prop] = parProps[prop];
            } else if(nodeName != 'a') {
                var runProps = styleProperties.runProps;
                for(prop in runProps) element.style[prop] = runProps[prop];
            }
        }
    };

    var _getCssShadow = function(shadow) {
        return !shadow.on ? "none"
            : shadow.offsetX + "px " + shadow.offsetY + "px " + shadow.blurRadius + "px " + _getCssColor(shadow.color);
    };

    var _getCssStyleProperties = function(style) {
        var toApply = {};
        toApply.runProps = {};
        toApply.parProps = {};
        toApply.allProps = {};

        if(style.fontName) toApply.allProps.fontFamily = toApply.runProps.fontFamily = style.fontName;
        // we need to set font size on both runs and pars because otherwise it well mess up the measure and thereby vertical alignment
        if(style.fontSize) toApply.allProps.fontSize = toApply.runProps.fontSize = toApply.parProps.fontSize = style.fontSize;
        if(style.fontWeight !== undefined) toApply.allProps.fontWeight = toApply.runProps.fontWeight = style.fontWeight;
        if(style.fontStyle !== undefined) toApply.allProps.fontStyle = toApply.runProps.fontStyle = style.fontStyle;

        var textDecoration = [];
        if(style.underline !== undefined) textDecoration[0] = style.underline ? 'underline ' : 'none';
        if(style.strikethrough !== undefined) {
            var index = textDecoration.length;
            if(style.strikethrough) textDecoration[index] ='line-through';
            else if(index == 0) textDecoration[0] = 'none';
        } 
        if (textDecoration.length > 0) {
            var decorationLineUp = "";
            for (var l = 0; l < textDecoration.length; l++) {
                decorationLineUp = decorationLineUp + textDecoration[l];
            }
            toApply.allProps.textDecoration = toApply.runProps.textDecoration = decorationLineUp;
        }
        if(style.foreGroundFill) {
            toApply.allProps.color = toApply.runProps.color = _getColorFromFill(style.foreGroundFill);
            //if(style.foreGroundFill.opacity) toApply.allProps.opacity = toApply.runProps.opacity = style.foreGroundFill.opacity;
        }
        if(style.horizontalAlignment) toApply.allProps.textAlign = toApply.parProps.textAlign = toApply.runProps.textAlign = style.horizontalAlignment;
        if(style.lineSpacing) toApply.allProps.lineHeight = toApply.parProps.lineHeight = style.lineSpacing;
        if(style.textShadow) toApply.allProps.textShadow = toApply.parProps.textShadow = _getCssShadow(style.textShadow);
        if (style.letterCase) toApply.allProps.textTransform = toApply.parProps.textTransform = style.letterCase;
        if (style.characterSpacing) toApply.allProps.letterSpacing = toApply.runProps.letterSpacing = style.characterSpacing;

        return toApply;
    };

    var _getColorFromFill = function(fill) {
        //var fillString = '00000' + fill.color.toString(16);
        //return '#' + fillString.substring(fillString.length - 6);
        var val = fill.color;
        var color = {};
        color.b = val % 256;
        val = Math.floor(val / 256);
        color.g = val % 256;
        val = Math.floor(val / 256);
        color.r = val % 256;
        color.a = typeof (fill.opacity) == 'number' ? fill.opacity : 1;
        return _getCssColor(color);
    };

    var _getColorFromArgbColorProp = function (val) {
        var color = {};
        color.b = val % 256;
        val = Math.floor(val / 256);
        color.g = val % 256;
        val = Math.floor(val / 256);
        color.r = val % 256;
        val = Math.floor(val / 256);
        color.a = val % 256;
        return _getCssColor(color);
    };

    var _getCssColor = function(rgbaObj) {
        return "rgba(" + rgbaObj.r + ", " + rgbaObj.g + ", " + rgbaObj.b + ", " + rgbaObj.a + ")";
    };

    //    //--------------------------------------------------------------------------
    //    // ApplyStyleRecursive
    //    //
    //    // Applies a style recursively to all span and div tags including elementNode
    //    // and all of its children.
    //    //
    //    //     element : the element to apply the style to
    //    //     styleName : the name of the style property to set (eg. 'font-weight')     
    //    //     styleValue : the value of the style to set (eg. 'bold')
    //    //--------------------------------------------------------------------------
    //    function ApplyStyleRecursive(element, styleName, styleValue) {
    //        var nodeName = element.nodeName.toLowerCase();

    //        if (nodeName == 'div' || nodeName == 'span' || nodeName == 'p') {
    //            element.style[styleName] = styleValue;
    //        }

    //        for (var i = 0; i < element.childNodes.length; i++) {
    //            ApplyStyleRecursive(element.childNodes[i], styleName, styleValue);
    //        }
    //    }

    //    //---------------------------------------------------------------------------
    //    // ApplyTextProperty
    //    //
    //    // Applies a text property to rtfElement.
    //    //
    //    //     rtfElement : the the root text element of the rtf object (this is the
    //    //                  element named <id>_rtf
    //    //     prop : the style property to set.
    //    //     value : the style value to set.
    //    //---------------------------------------------------------------------------
    //    function ApplyTextProperty(rtfElement, prop, value) {
    //        /*
    //        var oldHtml = rtfElement.innerHTML;
    //        if (prop == 'fontWeight') {
    //            rtfElement.innerHTML = oldHtml.replace(/< *b *\/?>/gi, "");
    //        } else if (prop == 'fontStyle') {
    //            rtfElement.innerHTML = oldHtml.replace(/< *i *\/?>/gi, "");
    //        } else if (prop == 'textDecoration') {
    //            rtfElement.innerHTML = oldHtml.replace(/< *u *\/?>/gi, "");
    //        }
    //        */

    //        for (var i = 0; i < rtfElement.childNodes.length; i++) {
    //            ApplyStyleRecursive(rtfElement.childNodes[i], prop, value);
    //        }
    //    }
    //}

    //---------------------------------------------------------------------------
    // GetAndCacheOriginalText
    //
    // Gets the html for the pre-rollover state and returns the Html representing
    // the Rich text.
    //---------------------------------------------------------------------------
    var CACHE_COUNTER = 0;

    $ax.style.CacheOriginalText = function(textId, hasRichTextBeenSet) {
        var rtfQuery = $('#' + textId);
        if(rtfQuery.length > 0) {

            var styleCache = {};
            rtfQuery.find('*').each(function(index, element) {
                var elementId = element.id;
                if(!elementId) element.id = elementId = 'cache' + CACHE_COUNTER++;
                styleCache[elementId] = element.style.cssText;
            });

            _originalTextCache[textId] = {
                styleCache: styleCache
            };
            if(hasRichTextBeenSet) {
                var shapeId = $ax.GetShapeIdFromText(textId);
                _shapesWithSetRichText[shapeId] = true;
            }
        }
    };

    $ax.style.ClearCacheForRepeater = function(repeaterId) {
        for(var elementId in _originalTextCache) {
            var scriptId = $ax.repeater.getScriptIdFromElementId(elementId);
            if($ax.getParentRepeaterFromScriptId(scriptId) == repeaterId) delete _originalTextCache[elementId];
        }
    };



    $ax.style.prefetch = function() {
        var scriptIds = $ax.getAllScriptIds();
        var image = new Image();
        for(var i = 0; i < scriptIds.length; i++) {
            var obj = $obj(scriptIds[i]);
            if (!$ax.public.fn.IsImageBox(obj.type)) continue;
            var images = obj.images;
            for (var key in images) image.src = images[key];

            var imageOverrides = obj.imageOverrides;
            for(var elementId in imageOverrides) {
                var override = imageOverrides[elementId];
                for (var state in override) {
                    _addImageOverride(elementId, state, override[state]);
                    image.src = override[state];
                }
            }
        }
    };
});
