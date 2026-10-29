
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Add Product - Shop</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
        }

        form {
            max-width: 450px;
        }

        label {
            display: block;
            margin-top: 16px;
        }

        input {
            width: 100%;
            padding: 9px;
            box-sizing: border-box;
        }

        button, a {
            display: inline-block;
            margin-top: 20px;
            padding: 9px;
        }

        .error {
            color: #c62828;
            margin-top: 5px;
        }
    </style>
</head>
<body>
    <h1>Add Product</h1>

    <form action="${pageContext.request.contextPath}/products"
          method="post">

        <label for="name">Product name</label>
        <input id="name" name="name" type="text" maxlength="80"
               value="<c:out value='${product.name}'/>" required>

        <c:if test="${errors.hasFieldErrors('name')}">
            <p class="error">
                <c:out value="${errors.getFieldError('name').defaultMessage}"/>
            </p>
        </c:if>

        <label for="price">Price</label>
        <input id="price" name="price" type="number" min="0.01"
               step="0.01"
               value="<c:out value='${product.price}'/>" required>

        <c:if test="${errors.hasFieldErrors('price')}">
            <p class="error">
                <c:out value="${errors.getFieldError('price').defaultMessage}"/>
            </p>
        </c:if>

        <label for="stock">Stock</label>
        <input id="stock" name="stock" type="number" min="0"
               value="<c:out value='${product.stock}'/>" required>

        <c:if test="${errors.hasFieldErrors('stock')}">
            <p class="error">
                <c:out value="${errors.getFieldError('stock').defaultMessage}"/>
            </p>
        </c:if>

        <button type="submit">Create Product</button>
        <a href="${pageContext.request.contextPath}/products">Cancel</a>
    </form>
</body>
</html>
