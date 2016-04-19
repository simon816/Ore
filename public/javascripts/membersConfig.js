var KEY_ENTER = 13;

function updateIndices() {
    // Set the input fields to their proper indices so the server can read
    // them as a list.
    var rows = $('.table-members').find('tr');
    rows.each(function(i) {
        if (i == 0 || i == rows.length - 1) return; // Skip owner and search rows
        var index = i - 1;
        $(this).find('input').attr('name', 'users[' + index + ']');
        $(this).find('select').attr('name', 'roles[' + index + ']');
    });
}

function initUserSearch(element) {
    var input = element.find('input');
    var btn = element.find('.btn');

    // Disable button with no input
    input.on('input', function() {
        $(this).next().find('.btn').prop('disabled', $(this).val().length == 0);
    });

    // Catch "enter" action
    input.on('keypress', function(event) {
        if (event.keyCode === KEY_ENTER) {
            event.preventDefault();
            btn.click();
        }
    });

    // Replace input with result
    btn.click(function() {

        var row = $(this).closest('tr');
        var inputGroup = row.find('.user-search');
        var btnIcon = $(this).find('i');
        var input = inputGroup.find('input');
        var username = input.val(); // Submitted name

        // Request user on click
        btnIcon.removeClass('fa-search').addClass('fa-spinner fa-spin');
        $.ajax({
            url: '/api/users/' + username,
            dataType: 'json',

            complete: function() {
                input.val('');
                btnIcon.removeClass('fa-spinner fa-spin').addClass('fa-search').prop('disabled', true);
            },

            error: function() {
                $('.error-username').text(username);
                $('.alert-danger').fadeIn();
            },

            success: function(user) {
                // Build the result row from the template
                var newRow = $('#result-row').clone().attr('id', '');
                newRow.find('input').attr('form', 'form-continue').val(user.id);
                newRow.find('select').attr('form', 'form-continue');
                newRow.find('a').attr('href', '/' + user.username).text(user.username);

                // Bind cancel button
                newRow.find('.user-cancel').click(function() {
                    $(this).closest('tr').remove();
                    updateIndices();
                });

                // Insert the new row before the search row
                row.before(newRow);
                updateIndices();
            }
        });
    });
}

$(function() {
    initUserSearch($('.user-search'));
    updateIndices();
});
