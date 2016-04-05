function validateMeta(pluginId, owner, name, baseUrl) {
    checkId(pluginId, owner, name, baseUrl)
}

function tooltip(selector, title) {
    $(selector).tooltip({
        placement: 'right',
        title: title
    });
}

function removeSpinner(selector) {
    $(selector).removeClass('fa-spinner fa-spin');
}

function success(selector) {
    removeSpinner(selector);
    $(selector).addClass('fa-check-circle');
}

function failed(selector, message) {
    removeSpinner(selector);
    $(selector).addClass('fa-times-circle');
    tooltip(selector, message);
}

function checkId(pluginId, owner, name, baseUrl) {
    $.ajax({
        url: baseUrl + '/api/project?pluginId=' + pluginId,
        statusCode: {
            404: function() {
                success('.id-status');
                checkName(owner, name, true, baseUrl);
            },
            200: function() {
                failed('.id-status', 'That plugin ID is not available!');
                checkName(owner, name, false, baseUrl);
            }
        }
    })
}

function checkName(owner, name, idSuccess, baseUrl) {
    if (name.length > 25) {
        failed('.name-status', 'This name is too long. Please rename your project to something under 25 characters.');
        updateContinueButton(idSuccess, false);
        return;
    }

    $.ajax({
        url: baseUrl + '/' + owner + '/' + name,
        statusCode: {
            404: function() {
                success('.name-status');
                updateContinueButton(idSuccess, true);
            },
            200: function() {
                failed('.name-status', 'You already have a project of this name!');
                updateContinueButton(idSuccess, false);
            }
        }
    });
}

function updateContinueButton(idSuccess, nameSuccess) {
    if (idSuccess && nameSuccess) {
        $('.continue-btn').prop('disabled', false);
    }
};
