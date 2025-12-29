// static/js/transaction-form-modal.js
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('transactionForm');
    const saveButton = document.getElementById('saveTransactionBtn');

    if (saveButton && form) {
        const modalElement = document.getElementById('transactionConfirmModal');
        if (typeof bootstrap !== 'undefined' && bootstrap.Modal && modalElement) {
            const transactionModal = new bootstrap.Modal(modalElement);

            const confirmDescriptionModal = document.getElementById('confirmDescriptionModal');
            const confirmAmountModal = document.getElementById('confirmAmountModal');
            const confirmDateModal = document.getElementById('confirmDateModal');
            const confirmTypeModal = document.getElementById('confirmTypeModal');
            const confirmCategoryModal = document.getElementById('confirmCategoryModal');
            const confirmFileModal = document.getElementById('confirmFileModal'); // Span to display selected file name

            const confirmSubmitButtonModal = document.getElementById('confirmSubmitButtonModal');

            saveButton.addEventListener('click', function (event) {
                // Validate the form first
                if (!form.checkValidity()) {
                    form.reportValidity(); // Display native HTML5 validation messages
                    return; // Stop if form is invalid
                }

                // Populate modal with form data
                if(confirmDescriptionModal) confirmDescriptionModal.textContent = form.description.value;
                if(confirmAmountModal) confirmAmountModal.textContent = parseFloat(form.amount.value).toFixed(2);
                if(confirmDateModal && form.transactionDateString) confirmDateModal.textContent = form.transactionDateString.value;
                if(confirmTypeModal) confirmTypeModal.textContent = form.type.value;
                if(confirmCategoryModal) confirmCategoryModal.textContent = form.category.value;

                // Populate file name in modal
                const fileInput = form.fileReceipt; // Get the file input element by its name/id on the form
                if (confirmFileModal && fileInput && fileInput.files && fileInput.files.length > 0) {
                    confirmFileModal.textContent = fileInput.files[0].name;
                } else if (confirmFileModal) {
                    confirmFileModal.textContent = "No file selected";
                }

                transactionModal.show();
            });

            if(confirmSubmitButtonModal) {
                confirmSubmitButtonModal.addEventListener('click', function () {
                    form.submit(); // Submit the main form
                });
            } else {
                console.error("Modal 'Confirm & Save' button (confirmSubmitButtonModal) not found.");
            }
        } else {
            if (!modalElement) console.error("Modal element with ID 'transactionConfirmModal' not found.");
            if (typeof bootstrap === 'undefined' || !bootstrap.Modal) console.error("Bootstrap Modal class not available or not loaded.");
        }
    } else {
        if (!form) console.error("Form with ID 'transactionForm' not found for modal setup.");
        // saveButton might not be present on all pages, so warning can be less critical
        // if (!saveButton) console.warn("Save button with ID 'saveTransactionBtn' not found for modal setup on this page.");
    }
});