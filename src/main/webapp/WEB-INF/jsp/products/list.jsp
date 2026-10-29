
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Products - Shop</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
        th { background: #f2f2f2; }
        input, button, a { padding: 8px; }
        .message { color: green; }
    </style>
</head>
<body>
    <h1>Product List</h1>

    <c:if test="${not empty message}">
        <p class="message"><c:out value="${message}"/></p>
    </c:if>

    <form action="${pageContext.request.contextPath}/products" method="get">
        <input type="text" name="q" value="<c:out value='${q}'/>"
               placeholder="Search products">
        <button type="submit">Search</button>
        <a href="${pageContext.request.contextPath}/products">Clear</a>
    </form>

    <p>
        <a href="${pageContext.request.contextPath}/products/new">Add Product</a>
    </p>

    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Price</th>
                <th>Stock</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="product" items="${products}">
                <tr>
                    <td><c:out value="${product.id}"/></td>
                    <td><c:out value="${product.name}"/></td>
                    <td><c:out value="${product.price}"/></td>
                    <td><c:out value="${product.stock}"/></td>
                </tr>
            </c:forEach>

            <c:if test="${empty products}">
                <tr>
                    <td colspan="4">No products found.</td>
                </tr>
            </c:if>
        </tbody>
    </table>
</body>
</html>
