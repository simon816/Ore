var validate = function(pluginId, owner, name) {
    checkId(pluginId, owner, name)
};

var checkId = function(pluginId, owner, name) {
    $.ajax({
        url: "http://localhost:9000/api/projects/search?pluginId=" + pluginId,
        statusCode: {
            404: function() {
                $(".id-status").removeClass("fa-spinner fa-spin").addClass("fa-check-circle");
                checkName(owner, name, true);
            },
            200: function() {
                $(".id-status").removeClass("fa-spinner fa-spin").addClass("fa-times-circle");
                checkName(owner, name, false);
            }
        }
    })
};

var checkName = function(owner, name, idSuccess) {
    $.ajax({
        url: "http://localhost:9000/" + owner + "/" + name,
        statusCode: {
            404: function() {
                $(".name-status").removeClass("fa-spinner fa-spin").addClass("fa-check-circle");
                updateContinueButton(idSuccess, true);
            },
            200: function() {
                $(".name-status").removeClass("fa-spinner fa-spin").addClass("fa-times-circle");
                updateContinueButton(idSuccess, false);
            }
        }
    });
};

var updateContinueButton = function(idSuccess, nameSuccess) {
    if (idSuccess && nameSuccess) {
        $(".continue-btn").prop("disabled", false);
    }
};

